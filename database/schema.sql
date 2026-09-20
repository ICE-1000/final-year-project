-- WARNING: This schema reflects the live Supabase database as reviewed. It is for
-- context/reference - the tables already exist. Use MIGRATION-add-constraints.sql
-- to apply what's currently missing.

CREATE TABLE public.departments (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  department_name character varying NOT NULL,
  department_code character varying NOT NULL UNIQUE,
  created_at timestamp without time zone DEFAULT now(),
  CONSTRAINT departments_pkey PRIMARY KEY (id)
);
CREATE TABLE public.users (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  username character varying NOT NULL UNIQUE,
  email character varying NOT NULL UNIQUE,
  password character varying NOT NULL,
  role character varying NOT NULL CHECK (role::text = ANY (ARRAY['ADMIN'::character varying, 'DEPARTMENT'::character varying]::text[])),
  department_id uuid,
  created_at timestamp without time zone DEFAULT now(),
  deleted boolean DEFAULT false,
  CONSTRAINT users_pkey PRIMARY KEY (id),
  CONSTRAINT users_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id)
);
CREATE TABLE public.categories (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  name character varying NOT NULL UNIQUE,
  description text,
  created_at timestamp without time zone DEFAULT now(),
  code character varying NOT NULL UNIQUE,
  next_inventory_sequence bigint NOT NULL DEFAULT 1,
  CONSTRAINT categories_pkey PRIMARY KEY (id)
);
CREATE TABLE public.inventory (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  barcode character varying NOT NULL UNIQUE,
  inventory_name character varying NOT NULL,
  category character varying,
  description text,
  quantity integer NOT NULL DEFAULT 0,
  available_quantity integer NOT NULL DEFAULT 0,
  allocated_quantity integer NOT NULL DEFAULT 0,
  serial_number character varying,
  condition character varying,
  status character varying DEFAULT 'AVAILABLE'::character varying,
  barcode_image_url text,
  created_at timestamp without time zone DEFAULT now(),
  deleted boolean DEFAULT false,
  version bigint NOT NULL DEFAULT 0,
  category_id uuid,
  CONSTRAINT inventory_pkey PRIMARY KEY (id),
  CONSTRAINT inventory_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.categories(id)
);
CREATE TABLE public.allocations (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  inventory_id uuid,
  department_id uuid,
  quantity integer NOT NULL,
  allocated_by uuid,
  allocated_at timestamp without time zone DEFAULT now(),
  status character varying DEFAULT 'PENDING'::character varying,
  allocation_barcode character varying UNIQUE,
  allocation_barcode_image_url text,
  CONSTRAINT allocations_pkey PRIMARY KEY (id),
  CONSTRAINT allocations_inventory_id_fkey FOREIGN KEY (inventory_id) REFERENCES public.inventory(id),
  CONSTRAINT allocations_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id),
  CONSTRAINT allocations_allocated_by_fkey FOREIGN KEY (allocated_by) REFERENCES public.users(id)
);
CREATE TABLE public.inventory_history (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  inventory_id uuid,
  action_type character varying NOT NULL,
  quantity integer,
  performed_by uuid,
  timestamp timestamp without time zone DEFAULT now(),
  CONSTRAINT inventory_history_pkey PRIMARY KEY (id),
  CONSTRAINT inventory_history_inventory_id_fkey FOREIGN KEY (inventory_id) REFERENCES public.inventory(id),
  CONSTRAINT inventory_history_performed_by_fkey FOREIGN KEY (performed_by) REFERENCES public.users(id)
);
CREATE TABLE public.reports (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  report_type character varying,
  generated_by uuid,
  generated_at timestamp without time zone DEFAULT now(),
  file_url text,
  CONSTRAINT reports_pkey PRIMARY KEY (id),
  CONSTRAINT reports_generated_by_fkey FOREIGN KEY (generated_by) REFERENCES public.users(id)
);
CREATE TABLE public.inventory_requests (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  department_id uuid NOT NULL,
  item_name character varying NOT NULL,
  quantity integer NOT NULL CHECK (quantity > 0),
  needed_by date NOT NULL,
  description text,
  status character varying NOT NULL DEFAULT 'PENDING'::character varying,
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now(),
  rejection_reason text,
  created_by uuid,
  category_id uuid,
  CONSTRAINT inventory_requests_pkey PRIMARY KEY (id),
  CONSTRAINT inventory_requests_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id),
  CONSTRAINT inventory_requests_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id),
  CONSTRAINT inventory_requests_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.categories(id)
);
CREATE TABLE public.department_registrations (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  department_name character varying NOT NULL,
  department_code character varying NOT NULL UNIQUE,
  username character varying NOT NULL UNIQUE,
  email character varying NOT NULL UNIQUE,
  password character varying NOT NULL,
  status character varying NOT NULL DEFAULT 'PENDING'::character varying CHECK (status::text = ANY (ARRAY['PENDING'::character varying::text, 'APPROVED'::character varying::text, 'REJECTED'::character varying::text])),
  rejection_reason text,
  created_at timestamp without time zone DEFAULT now(),
  CONSTRAINT department_registrations_pkey PRIMARY KEY (id)
);
