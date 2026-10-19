-- ============================================================================
-- MIGRATION: unit-tracking
-- Adds individual-unit tracking (brand/specification on inventory groups, plus a new
-- inventory_units table for per-physical-item barcodes).
--
-- NOTE: with spring.jpa.hibernate.ddl-auto=update (already set), the backend will
-- apply all of this automatically on startup - you do NOT need to run this file by
-- hand against a normal deployment. It's provided for reference/documentation, and
-- for anyone who prefers to review and run schema changes manually before deploying.
-- All statements below are safe to run even if some/all of them were already applied
-- automatically (IF NOT EXISTS / idempotent guards throughout).
--
-- The one thing Hibernate's auto-update will NOT do for you is backfill units for
-- inventory rows that already existed before this feature - that part runs as Java
-- code (InventoryUnitBackfillConfig) automatically on the app's next startup, not as
-- SQL here.
-- ============================================================================

ALTER TABLE public.inventory ADD COLUMN IF NOT EXISTS brand character varying(150);
ALTER TABLE public.inventory ADD COLUMN IF NOT EXISTS specification character varying(150);
ALTER TABLE public.inventory ADD COLUMN IF NOT EXISTS next_unit_sequence bigint;

ALTER TABLE public.categories ADD COLUMN IF NOT EXISTS next_batch_sequence bigint;

CREATE TABLE IF NOT EXISTS public.inventory_batches (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  batch_code character varying(50) NOT NULL UNIQUE,
  inventory_id uuid NOT NULL,
  quantity_registered integer NOT NULL,
  registered_by uuid,
  registered_at timestamp without time zone DEFAULT now(),
  CONSTRAINT inventory_batches_pkey PRIMARY KEY (id),
  CONSTRAINT inventory_batches_inventory_id_fkey FOREIGN KEY (inventory_id) REFERENCES public.inventory(id),
  CONSTRAINT inventory_batches_registered_by_fkey FOREIGN KEY (registered_by) REFERENCES public.users(id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_batches_inventory_id ON public.inventory_batches(inventory_id);

CREATE TABLE IF NOT EXISTS public.inventory_units (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  inventory_id uuid NOT NULL,
  unit_number integer NOT NULL,
  unit_barcode character varying(150) NOT NULL UNIQUE,
  unit_barcode_image_url text,
  status character varying(20) NOT NULL DEFAULT 'AVAILABLE',
  allocation_id uuid,
  batch_id uuid,
  created_at timestamp without time zone DEFAULT now(),
  CONSTRAINT inventory_units_pkey PRIMARY KEY (id),
  CONSTRAINT inventory_units_inventory_id_fkey FOREIGN KEY (inventory_id) REFERENCES public.inventory(id),
  CONSTRAINT inventory_units_allocation_id_fkey FOREIGN KEY (allocation_id) REFERENCES public.allocations(id),
  CONSTRAINT inventory_units_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.inventory_batches(id),
  CONSTRAINT inventory_units_status_check CHECK (status IN ('AVAILABLE', 'ALLOCATED'))
);

CREATE INDEX IF NOT EXISTS idx_inventory_units_inventory_id ON public.inventory_units(inventory_id);
CREATE INDEX IF NOT EXISTS idx_inventory_units_allocation_id ON public.inventory_units(allocation_id);
CREATE INDEX IF NOT EXISTS idx_inventory_units_batch_id ON public.inventory_units(batch_id);

-- The old per-allocation-event barcode columns are no longer used by the application
-- (barcodes now live permanently on inventory_units instead) but are left in place,
-- untouched, so no historical data is lost:
--   allocations.allocation_barcode
--   allocations.allocation_barcode_image_url
-- Safe to drop later once you've confirmed you don't need that history, but not
-- required - the app simply ignores them now.
