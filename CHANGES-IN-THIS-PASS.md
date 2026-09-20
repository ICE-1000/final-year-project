# Changes in this pass

This is a full re-wiring pass across the backend, frontend, and database, based on a
line-by-line review of every file in the project. It builds on top of the fixes already
described in your existing `changes.md` (kept as historical record - not reproduced
here) and closes the gaps found on top of that.

## ⚠️ Action required before you deploy this

1. **Check the DB password in your `_env.example`.** An earlier version of this file
   had what looked like a real, live-looking Supabase password committed as a
   "placeholder." This version replaces it with an obvious placeholder
   (`your-db-password-here`). If that value was ever real, rotate it in the Supabase
   dashboard regardless of anything else here - treat it as compromised.
2. **Redeploy the backend and run `database/MIGRATION-add-constraints.sql`.** The
   categories 404 and the missing allocation barcodes you saw were both symptoms of
   the same thing: the live Render deployment predates the categories/barcode-scheme
   feature entirely. Rebuild and redeploy from this code, and run both
   `MIGRATION-categories.sql`/`MIGRATION-barcode-scheme.sql` (from your existing
   `changes.md`, if not already applied) and the new `MIGRATION-add-constraints.sql`
   in this project, in that order.
3. **Set `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS`**
   as real environment variables in your hosting platform - see `_env.example`.

## Fixed in this pass

### Backend - correctness
- **`InventoryService` read methods now `@Transactional(readOnly = true)`** -
  `findAll()`, `findByCategory()`, `findByBarcode()`, `tryFindByBarcode()`. With
  `spring.jpa.open-in-view=false` (already set), these were previously touching a lazy
  `@ManyToOne` (`category`) after the Hibernate session had already closed, which would
  throw `LazyInitializationException` on `GET /api/inventory` - the single most-used
  read endpoint in the app - for any item with a category set (i.e. all of them).
- **`InventoryDTO.availableQuantity`/`allocatedQuantity` changed from primitive `int`
  to boxed `Integer`**, and `InventoryService.create()`/`update()` rewritten around
  explicit null-checks instead of the old `available>0 || allocated>0 ? ... : quantity`
  heuristic. Previously, a partial-edit PUT that omitted these fields deserialized them
  as `0` - indistinguishable from an explicit zero - silently wiping an item's real
  stock numbers on any edit that didn't resend the full quantities. `null` now
  unambiguously means "not specified": create() defaults sensibly, update() leaves the
  existing value alone. `admin.js`'s inventory form was already only sending the fields
  it collects (not quantities on edit), so no frontend change was needed there - it now
  gets the "leave unchanged" behavior it always should have had.
- **`AllocationService.toDto()` now null-guards `inventory`/`department`** before
  reading their id/name, matching the pattern already used in
  `InventoryService`/`InventoryRequestService`. Both FKs are nullable at the DB level;
  previously one malformed row would NPE the entire `GET /api/allocations` list for
  everyone, not just that row.
- **`DepartmentDTO` gained validation** (`@NotBlank` name/code,
  `@Pattern` on code), matching the pattern `CategoryDTO` already used.
  `DepartmentController.create()` now has `@Valid`. `DepartmentService.create()` now
  trims/uppercases the code and checks uniqueness explicitly instead of relying solely
  on the DB constraint - so a blank or duplicate department name/code now returns a
  clean 400 instead of a raw 409.
- **`UserService` now blocks deleting or demoting the last remaining ADMIN** - previously
  nothing stopped an admin from locking every admin-only endpoint with no way back in
  short of a direct database edit.
- **`GlobalExceptionHandler`** gained two defensive handlers: `LazyInitializationException`
  (belt-and-suspenders on top of the `@Transactional` fix above) and
  `IllegalArgumentException` (turns a bad `Enum.valueOf()` value, e.g. from a stray
  legacy status string, into a clean 400 instead of falling through to a generic 500).

### Backend - repository / query
- `InventoryRepository.findByCategoryIdAndDeletedFalseOrderByCreatedAtDesc` and all
  three read methods on `InventoryRequestRepository` now fetch-join their `category`/
  `department` associations, avoiding N+1 queries (previously only the department-scoped
  request lookup did this).
- Removed `InventoryRequestRepository.findByDepartmentIdOrderByCreatedAtDesc` - dead
  code; `InventoryRequestService` only ever called the fetch-joined variant.
- Added `DepartmentRepository.existsByDepartmentCodeIgnoreCase`, used by the
  `DepartmentService.create()` fix above.

### Configuration
- **`jwt.secret` default lengthened.** The previous default was 53 characters -
  under the 64-byte minimum `JwtTokenProvider`'s own startup check requires for HS512 -
  so any fresh clone that didn't set `JWT_SECRET` manually would crash on startup with
  `IllegalStateException`, not run with a weak default. Fixed default is long enough to
  pass that check; production should still always override it.
- **CORS trailing slash removed** from the default `unza.netlify.app` origin
  (`CorsConfig` does an exact string match against the browser's `Origin` header, which
  never includes a trailing slash - the old value would have silently never matched in
  production). Also added defensive trailing-slash stripping in both `CorsConfig` and
  `WebSocketConfig` so a stray slash in a future config value doesn't reintroduce this.
- `_env.example`: replaced what looked like a real DB password with an obvious
  placeholder (see action-required item #1 above).

### Database
New `database/MIGRATION-add-constraints.sql`:
- `CHECK` constraints added to `inventory.status`, `allocations.status`, and
  `inventory_requests.status` - `users.role` and `department_registrations.status`
  already had these, the other three didn't. Defense-in-depth: the Java enum layer
  already prevents bad values through the app, this prevents them at the DB level too.
- Commented-out, manual-run `NOT NULL` tightening for `inventory_requests.category_id`
  and `allocations.inventory_id`/`department_id` - not applied automatically since
  it depends on whether any existing rows already have NULLs there; the file explains
  how to check first.
- One-time backfill for `allocation_barcode`/`allocation_barcode_image_url` on
  historical allocations that predate the barcode scheme (the rows that showed "-" in
  the Allocation Barcode column) - regenerates a barcode for each, using that
  allocation's own `allocated_at` year rather than the current year, so it stays
  meaningful to when the allocation actually happened.

### Frontend
- **`department-requests.js`: fixed the `neededBy` date bug.** The `<input type="month">`
  + `-01` combination made picking the *current* month fail the backend's
  `@FutureOrPresent` validation on any day after the 1st (e.g. picking "September 2026"
  on September 17 produces "2026-09-01," which is in the past). Now: if the selected
  month is the current month, the actual current date is sent instead of the 1st.
- **Removed `requests.js`** - dead/unreferenced code. No HTML page loaded it (admin
  pages use `admin-requests.js`, department pages use `department-requests.js`); it
  also lacked `categoryId` handling entirely, so reconnecting it by mistake would have
  broken every request submission against the now-required `categoryId` field.
- **Removed the dead `deleteDepartment()` helper from `admin.js`** - `departments.html`
  handles its own delete flow inline and never loaded `admin.js`, so this was
  unreferenced.
- **`users.html` rebuilt** (the version I had in context was incomplete) from the
  `UserController`/`UserUpdateRequest` contract, with the role-select sync bug fixed:
  opening the edit modal for an existing user now correctly shows/hides the department
  field based on that user's actual role, instead of only updating on a manual
  dropdown change.
- **`department/reports.html` now actually downloads reports** instead of showing a
  placeholder message - `ReportController`'s `/reports/department.pdf` and
  `/reports/department.xlsx` endpoints already existed and already worked for a
  DEPARTMENT caller; the frontend just never called them.

## Not changed / left as-is
- Everything not listed above was reviewed and found correct as originally written -
  security config, JWT handling, the barcode/allocation composite-ID scheme, the
  category sequence-counter locking, referential-integrity guards on delete, and the
  rest of the frontend all check out and were carried over unchanged.
- `Report`/`ReportRepository` remain unused by `ReportService` (reports are generated
  on demand, never persisted as a `Report` row) - this looked intentional rather than
  a bug, but worth a deliberate decision on your end about whether that's in scope.
- `inventory_history` is still only written from allocations, not from inventory
  creation/edits/deletes - same as above, a scope decision rather than a bug fix.
