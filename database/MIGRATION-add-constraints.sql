-- ============================================================================
-- MIGRATION: add-constraints
-- Run this against the live Supabase database. Safe to re-run (each statement
-- checks for existence first) except the backfill UPDATE at the end, which is
-- naturally idempotent (it only touches rows where allocation_barcode IS NULL).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. CHECK constraints on status columns.
-- users.role and department_registrations.status already have these; inventory.status,
-- allocations.status, and inventory_requests.status don't. The Java @Enumerated layer
-- enforces valid values today, but nothing stops a bad migration, a manual edit, or a
-- future bug from inserting a stray value directly - which the app can then fail to
-- deserialize with an unmapped-enum error. This closes that gap at the DB level too.
-- ----------------------------------------------------------------------------

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'inventory_status_check'
    ) THEN
        ALTER TABLE public.inventory
            ADD CONSTRAINT inventory_status_check
            CHECK (status IN ('AVAILABLE', 'ALLOCATED'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'allocations_status_check'
    ) THEN
        ALTER TABLE public.allocations
            ADD CONSTRAINT allocations_status_check
            CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'inventory_requests_status_check'
    ) THEN
        ALTER TABLE public.inventory_requests
            ADD CONSTRAINT inventory_requests_status_check
            CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));
    END IF;
END $$;


-- ----------------------------------------------------------------------------
-- 2. OPTIONAL / MANUAL: tighten nullability on columns the application always sets.
-- These are commented out deliberately - run them yourself ONLY after confirming
-- there are no existing NULL rows (the SELECT checks below will tell you). If any
-- of these return rows, decide how to backfill/handle them before adding NOT NULL,
-- or the ALTER will fail outright.
-- ----------------------------------------------------------------------------

-- Check first:
-- SELECT count(*) FROM public.inventory_requests WHERE category_id IS NULL;
-- SELECT count(*) FROM public.allocations WHERE inventory_id IS NULL OR department_id IS NULL;

-- Then, only if both return 0:
-- ALTER TABLE public.inventory_requests ALTER COLUMN category_id SET NOT NULL;
-- ALTER TABLE public.allocations ALTER COLUMN inventory_id SET NOT NULL;
-- ALTER TABLE public.allocations ALTER COLUMN department_id SET NOT NULL;


-- ----------------------------------------------------------------------------
-- 3. One-time backfill: historical allocations created before the composite
-- barcode scheme was added have allocation_barcode = NULL (this is what showed
-- up as "-" in the Allocation Barcode column on the frontend). This regenerates
-- one for every such row, using each row's own allocated_at year (not the
-- current year) so the barcode stays meaningful to when the allocation actually
-- happened. Only touches rows where allocation_barcode IS NULL - safe to re-run.
-- ----------------------------------------------------------------------------

UPDATE public.allocations a
SET allocation_barcode =
    EXTRACT(YEAR FROM a.allocated_at)::text || '-' ||
    COALESCE(c.code, 'NA') || '-' ||
    i.barcode || '-' ||
    d.department_code || '-' ||
    UPPER(SUBSTRING(REPLACE(gen_random_uuid()::text, '-', ''), 1, 6))
FROM public.inventory i
JOIN public.departments d ON d.id = a.department_id
LEFT JOIN public.categories c ON c.id = i.category_id
WHERE a.inventory_id = i.id
  AND a.department_id = d.id
  AND a.allocation_barcode IS NULL;

UPDATE public.allocations
SET allocation_barcode_image_url = '/api/barcode/image/' || allocation_barcode
WHERE allocation_barcode IS NOT NULL
  AND allocation_barcode_image_url IS NULL;

-- Sanity check after running: this should return 0 once done (any remainder means
-- some allocations reference an inventory_id/department_id that no longer exists,
-- or itself has a NULL - the AllocationService.toDto() null guard now handles that
-- gracefully on the read side, but you may want to investigate those rows).
-- SELECT count(*) FROM public.allocations WHERE allocation_barcode IS NULL;
