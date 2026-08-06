# Changes in this version

This is a security/reliability pass over the existing IMS backend, plus two
new features you asked for: per-department report downloads and a proper
barcode assign/scan flow. Nothing in the API surface was renamed or removed
except where explicitly noted below - existing frontend calls should keep
working unchanged, aside from the new auth restriction on `/api/auth/register`.

## ⚠️ Action required before you deploy this

1. **Rotate your Supabase database password.** The old `application.properties`
   had it committed in plain text. Even though this new version removes it,
   the old value may still be sitting in your git history - treat it as
   already compromised and change it in the Supabase dashboard.
2. **Set `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`** as real
   environment variables in your hosting platform (see `.env.example`).
   The app will now **fail to start** if `JWT_SECRET` or the DB credentials
   are missing - that's intentional (fail closed, not fail open).
   Generate a JWT secret with: `openssl rand -base64 64`
3. If this is a brand-new database with zero users, set
   `ADMIN_BOOTSTRAP_USERNAME` / `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD`
   once, so the app can create your first admin account on startup. If you
   already have an admin (which you do on the live system), leave these unset -
   it's a no-op.

## Security/correctness fixes

- **Closed the self-registration privilege-escalation hole.** `/api/auth/register`
  (which accepts a client-supplied `role`) now requires an authenticated ADMIN
  caller. Department self-registration (`/api/auth/department/register`) is
  unaffected - it still goes through the existing pending-approval flow and
  never lets the caller pick a role.
- **Fixed the allocation IDOR.** `GET /api/allocations/department/{id}` now
  checks that a DEPARTMENT caller can only request their own department; added
  `GET /api/allocations/me` as a convenience.
- **Added missing `@PreAuthorize`** to `DepartmentController` (create/delete),
  `InventoryController` (create/update/delete), and `ReportController`
  (system-wide report endpoints) - previously any authenticated user regardless
  of role could hit these.
- **Fixed the JWT filter** to catch exceptions (e.g. a token for a since-deleted
  user) instead of letting them escape as a raw error page.
- **Upgraded JWT library** from `jjwt 0.9.1` (2016, unmaintained) to
  `jjwt-api/impl/jackson 0.11.5`, with a startup check that the secret is long
  enough for HS512.
- **Added optimistic locking (`@Version`)** to `Inventory` so two concurrent
  allocations against the same item can no longer both succeed and
  over-allocate stock; a real conflict now returns a clean 409 instead of
  silent data corruption.
- **Added a referential-integrity check** before deleting a department (blocks
  deletion if it still has active users, allocations, or requests, with a
  clear error message instead of a raw DB constraint failure).
- **Converted `Inventory.status` and `Allocation.status`** from free-form
  strings to proper enums (`InventoryStatus`, `AllocationStatus`) - same values
  as before, so this doesn't change the JSON your frontend already sees.
- **Null-safety in report generation** - a missing category or other optional
  field can no longer crash the entire PDF/Excel export.
- **Password length validation** added to registration and user-update flows.
- Removed a duplicate set of dead wrapper methods in `UserService`.
- Fixed a stray trailing slash in the default CORS origin list that would
  have silently failed to match in production.
- Added `AccessDeniedException`, `ObjectOptimisticLockingFailureException`,
  and `DataIntegrityViolationException` handlers to `GlobalExceptionHandler`
  so all of the above return clean JSON errors instead of falling through to
  a generic 500.

## New: department & admin report downloads

- `GET /api/reports/inventory.pdf` / `.xlsx` - **ADMIN only.** Full
  system-wide inventory report (unchanged from before, now properly gated).
- `GET /api/reports/department.pdf` / `.xlsx` - **DEPARTMENT or ADMIN.**
  A department's own allocation report (what's been allocated to them, with
  quantity/status/date). A DEPARTMENT caller always gets their own department
  automatically; an ADMIN can pass `?departmentId=<uuid>` to pull any
  department's report.

Report layout is intentionally plain right now (a title + a table) since you
said you'd cover design/format later - the important part built now is that
the data is correctly scoped and null-safe.

## New: barcode assign / scan / retrieve

- **Assign:** `POST /api/inventory` no longer requires the caller to supply a
  barcode. If you omit it, the system generates one and guarantees it's unique
  against the database (retries a few times on the astronomically unlikely
  chance of a collision). If you do supply one, it's validated for uniqueness
  rather than trusted blindly. Barcodes are immutable after creation.
- **Scan / retrieve:** `GET /api/barcode/scan/{barcode}` looks up the matching
  inventory item from the database (the existing `GET /api/inventory/{barcode}`
  still works too - `/scan/` is a clearer name for a barcode-scanner frontend
  to call).
- `GET /api/barcode/new` is now just a preview/format generator, not tied to a
  database record - useful for previewing the format, not for real assignment.
- `GET /api/barcode/image/{text}` is unchanged (public, so it works in `<img>`
  tags).

## What I could not do in this sandbox

I don't have network access to Maven Central here, so I could not actually
run `mvn compile`/`mvn test` on this project - I reviewed every file by hand
instead. Please run a build locally or in CI before deploying
(`mvn clean verify`), and if anything doesn't compile, send me the error and
I'll fix it immediately.

## New: inventory categories

- New `Category` entity/table: `id`, `name` (unique), `description`, `createdAt`.
- `GET /api/categories` - any authenticated user (ADMIN and DEPARTMENT both need
  to read this list).
- `POST /api/categories` - **ADMIN only.** Add a new category.
- `DELETE /api/categories/{id}` - **ADMIN only.** Blocked with a clear error if
  any inventory item or inventory request still references it (same
  referential-integrity pattern as department deletion).
- `Inventory.category` changed from a free-text string to a required relation
  to `Category` - every item now belongs to exactly one category, chosen from
  the list an admin maintains, not typed freely.
- `InventoryRequest` also gained a `category` relation - a department picks a
  category when submitting a request, same as admin does when registering
  inventory.
- New `GET /api/inventory/category/{categoryId}` - lets a department (or admin)
  see what's currently available in a category: name, remaining quantity, and
  specs (description, serial number, condition) - meant to be called right
  after picking a category on the "Request Inventory" screen, before they
  decide what to submit.
- Barcode auto-generation now uses the item's category name as the prefix
  (e.g. `ELECTRONICS-...`) instead of the generic `INV-...` default.

**Migration required** - see `MIGRATION-categories.sql`. Run it in Supabase
before deploying this version. It creates the `categories` table, adds
`category_id` to both `inventory` and `inventory_requests`, and optionally
seeds a starter set of categories you can edit before running.

The old `inventory.category` text column is left in place, untouched and
unused by the application - see the SQL file for why, and how to drop it
later once you're comfortable.

## New: full barcode identity scheme (category / inventory / department / allocation)

- **Category**: now has a required unique `code` (e.g. `ELEC`), settable when the
  category is created. Printable on its own via `GET /api/barcode/image/{code}`.
- **Inventory**: auto-assigned barcodes are now sequential per category instead of
  timestamp+random, e.g. `ELEC-0001`, `ELEC-0002`, ... The counter lives on the
  `Category` row and is incremented under a database row lock
  (`CategoryRepository.findByIdForUpdate`), so two concurrent item creations in
  the same category can never collide - no retry loop needed.
- **Department**: already had a unique `departmentCode` from the original schema -
  this now doubles as the department's barcode component (printable via the same
  `GET /api/barcode/image/{departmentCode}` endpoint, nothing new needed there).
- **Allocation**: every allocation now gets a composite, printable barcode:
  `{year}-{categoryCode}-{inventoryBarcode}-{departmentCode}-{6-char suffix}`
  (e.g. `2026-ELEC-ELEC-0007-IT-9F3D2A`). The trailing suffix is not part of what
  was asked for verbatim - it's there because the four requested parts alone can
  collide (same item allocated to the same department twice in one year would
  otherwise produce an identical, unscannable-with-certainty code). Stored in
  new `Allocation.allocationBarcode` (unique) and rendered via the existing
  `GET /api/barcode/image/{text}` endpoint (works for any string, no change
  needed there).
- **Unified scan endpoint**: `GET /api/barcode/scan/{barcode}` now recognizes
  both formats. It tries an inventory-item match first, then falls back to an
  allocation match, and returns `{ type: "INVENTORY" | "ALLOCATION", inventory,
  allocation }` so the caller knows which one it got. 404s if neither matches.

**Migration required**: see `MIGRATION-barcode-scheme.sql` (run after
`MIGRATION-categories.sql`, before deploying). It adds the category `code` +
sequence columns (with a backfill step for the starter categories) and the
allocation barcode columns.
