# Individual unit tracking, brand/specification, and barcode label printing

This is a genuine data-model change, not a bugfix - it reworks how inventory is
represented so every physical item is individually trackable, on top of everything
from the earlier review-and-fix pass (see `CHANGES-IN-THIS-PASS.md`).

## The core idea

`Inventory` is now a **group** (category + name + brand + specification) rather than a
single trackable thing. A new `InventoryUnit` row represents each **physical item**
within that group - one specific pen out of a box of 50 - and gets its own permanent
barcode assigned once, at registration, that never changes for the rest of its
lifecycle regardless of where it's later allocated. This is how real asset tags work,
and it's what makes "scan this specific item and get everything about it" possible.

## What this gets you, mapped to what you asked for

- **"Each should have an incrementing number so every product can be identified
  uniquely."** Every unit gets `{group barcode}-{4-digit number}`, e.g. `ELEC-0007-0001`,
  `ELEC-0007-0002` ... The numbering is per-group and never restarts.
- **"When registering again for the same category, the quantity should increment the
  existing inventory number."** `POST /api/inventory` now checks for an existing group
  in the same category with a matching name + brand + specification (case-insensitive).
  If found, it adds the new quantity as more units to that same group, continuing the
  numbering from wherever it left off, instead of creating a duplicate row. If not
  found, it creates a new group and starts numbering at 1.
- **"Some inventory has different brands and specifications - add those fields, each
  identified uniquely in that group."** `brand` and `specification` are now real fields
  on every inventory group and part of what decides whether a registration matches an
  existing group or starts a new one. Two brands of the same item in the same category
  are correctly kept as separate groups, each with their own unit numbering.
- **"An arrow leading to a table showing the allocation barcodes for that allocation,
  each with a downloadable image."** Every allocation row in `admin/allocation.html`
  has a "View Barcodes" button that opens a modal listing every individual unit that
  was part of that allocation, each with its own barcode and a Download button.
- **"Since inventory is uniquely identified from registration, this should be the case
  for all - scanning returns full specifications, brand, everything."** `GET /api/barcode/
  scan/{barcode}` now resolves to a single unit and returns its full group context
  (name, category, brand, specification, condition, description) plus its own status
  and, if allocated, which department and when.
- **"A download option for barcode images, normal size to stick on anything."** Every
  unit barcode is downloadable individually (both from the scan result page and from
  any unit table) via the existing `/api/barcode/image/{code}` endpoint, unchanged in
  size (a Code128 barcode rendered at 300x100px, a standard label size at normal
  print resolution).
- **"A button to download all barcode images, categorised on paper with allocations so
  they're not mixed."** New `GET /api/reports/allocation-barcodes.pdf` generates a PDF
  with one clearly-headed section per allocation (department, item, date, quantity),
  each starting on its own page - so if you print it and cut the labels apart, units
  from different allocations are never mixed together. A matching "Download All
  Barcodes" button sits at the top of the Allocation page for all allocations at once,
  and each allocation's own "View Barcodes" modal has a scoped version of the same
  button for just that one allocation. The same pattern exists for a single inventory
  group via `GET /api/reports/inventory-barcodes.pdf?inventoryId=...` (a "print labels
  for everything I just registered" button), from the "View Units" action on the
  Inventory page.

## What changed, concretely

### Backend
- New `InventoryUnit` entity/table: one row per physical item, with its own barcode,
  status (AVAILABLE/ALLOCATED), and a link to whichever `Allocation` it's currently
  part of (if any).
- `Inventory` gained `brand`, `specification`, and `nextUnitSequence` (the per-group
  unit counter).
- `Allocation` no longer carries its own barcode fields - barcodes live permanently on
  `InventoryUnit` now, reused for the item's whole life rather than re-minted per
  allocation event. The old DB columns are left in place, unused, so no history is lost.
- `InventoryService.create()` now does the create-or-merge decision described above,
  under a pessimistic lock on the parent `Category` row for the whole operation - this
  is what guarantees two concurrent registrations of what would be the same brand-new
  item can never both think "no match exists" and create two duplicate groups.
- `InventoryService.update()` is now metadata-only (name/brand/specification/
  description/condition/serial). Quantity, barcode, and category can no longer be
  edited directly - quantity only changes by registering more stock (which merges into
  the matching group), and barcode/category are immutable once units exist.
- `AllocationService.allocate()` now selects the actual next-available units for the
  requested quantity, row-locked (`SELECT ... FOR UPDATE`), instead of just decrementing
  a counter - this means it's now impossible for two concurrent allocation requests
  against the same group to ever hand out the same physical unit, and "insufficient
  stock" errors are based on real per-unit availability rather than a counter that could
  in principle drift from reality.
- `GET /api/barcode/scan/{barcode}` is now unit-based (see above) - simpler and more
  capable than the old two-type inventory-barcode-or-allocation-barcode split.
- New endpoints: `GET /api/inventory/{id}/units`, `GET /api/allocations/{id}/units`,
  `GET /api/reports/allocation-barcodes.pdf` (optional `?allocationId=`), `GET /api/
  reports/inventory-barcodes.pdf?inventoryId=...`.
- `InventoryUnitBackfillConfig` - a new, idempotent startup task that generates units
  retroactively for any inventory group that existed before this feature, so nothing
  registered under the old system becomes stuck or unscannable. Pre-existing allocated
  stock is marked ALLOCATED but can't be tied back to a specific historical allocation
  (old allocations didn't track individual units) - new allocations from now on always
  will be.
- `PdfService` gained `generateBarcodeLabelSheet()` (the grouped/sectioned label-sheet
  generator used by both new report endpoints) and now embeds real barcode images
  (rendered fresh via `BarcodeService`, since there's no stored image file anywhere -
  "image URL" has always meant the live `/api/barcode/image/{text}` endpoint).
  `ExcelService`/`PdfService`'s existing inventory report also gained Brand/
  Specification columns.

### Frontend
- `admin/inventory.html`: added Brand + Specification fields to the registration form,
  with help text explaining the merge-on-re-registration behavior. The Quantity field
  is disabled with an explanatory label while editing an existing item (since quantity
  can only change via re-registration now). Added a "View Units" action per row.
- `admin/allocation.html`: replaced the old single "Allocation Barcode" column (which
  only ever showed one code for a whole multi-unit allocation) with a "View Barcodes"
  button opening the per-unit table, plus a page-level "Download All Barcodes (by
  Allocation)" button.
- New shared `frontend/js/units.js`: the unit-table rendering, per-unit download, and
  modal logic used by both the inventory and allocation "view barcodes" actions.
- `frontend/js/barcode.js`: scan result rendering unified into one function covering
  the new single unit-based result shape, with a "Download Barcode" button.
- `frontend/js/api.js`: added a shared `downloadFile()` helper (fetch-as-blob-then-save,
  works for authenticated/cross-origin resources reliably, unlike a plain `<a download>`
  tag) - both report pages were refactored to use it instead of their own copy.

### Database
`database/MIGRATION-unit-tracking.sql` - reference/documentation for the schema
changes above. Not required to run by hand: `spring.jpa.hibernate.ddl-auto=update`
applies all of it automatically on the backend's next startup. The one part that isn't
plain SQL - backfilling units for pre-existing inventory - runs as Java code
(`InventoryUnitBackfillConfig`) on that same startup, automatically.

## A design choice worth knowing about

Serial numbers stayed at the **group** level (not per-unit) - capturing a genuinely
unique serial for every individual unit at bulk-registration time (e.g. entering 50
separate serials when registering 50 pens) would be a much bigger UI lift than what was
asked for, and wasn't explicitly requested. If you do want true per-unit serial capture
later, that's a natural, contained extension of `InventoryUnit` (add a nullable
`serialNumber` column there, add an edit affordance in the unit table) - flag it and
I'll build it.

## Addendum: batch traceability + professional print output

Following review against an alternative design proposal, two more things were added
on top of everything above, while keeping the group-prefix + per-group unit numbering
scheme (`ELEC-0007-0001`) as originally built:

- **`InventoryBatch`**: a new record for every *registration event*, separate from the
  brand/spec group. If the same group is restocked three times across the year, that's
  three batch rows (`2026-ELEC-0001`, `2026-ELEC-0002`, ...), each with its own date,
  quantity, and the admin who registered it - so you can now answer both "which group
  does this unit belong to?" and "which specific registration produced it?" Every unit
  carries its batch code, visible in the scan result, the unit tables, and printed on
  its label. `GET /api/inventory/{id}/batches` and a "History" button on the Inventory
  page expose the full registration timeline for a group. Units backfilled from stock
  that pre-dates this feature have no batch (that history was never recorded) - new
  registrations always get one.
- **Professional label printing**: barcode labels in the PDF sheet are now sized to
  real-world label proportions (~65mm x 22mm image area) inside a bordered, cuttable
  cell that also prints the unit's code and batch reference beneath the barcode -
  rather than an arbitrary-sized image dropped on the page. A ZIP download (one PNG per
  unit, organized into folders per allocation/group) is available everywhere the PDF
  is, as an alternative for anyone who wants individual image files instead of a
  print-ready sheet.
