-- Migration: 20260807000000_mvp1_system_status.sql
-- Description: Create system_status singleton table for MVP 1B schema foundation

CREATE TABLE IF NOT EXISTS public.system_status (
    id INT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    status TEXT NOT NULL,
    backend_version TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Enable Row Level Security
ALTER TABLE public.system_status ENABLE ROW LEVEL SECURITY;

-- Insert singleton record
INSERT INTO public.system_status (id, status, backend_version, updated_at)
VALUES (1, 'online', '1', now())
ON CONFLICT (id) DO UPDATE 
SET status = 'online', backend_version = '1', updated_at = now();

-- Create RLS Policy for read access
DROP POLICY IF EXISTS "Allow public read access to system_status" ON public.system_status;
CREATE POLICY "Allow public read access to system_status"
    ON public.system_status
    FOR SELECT
    TO anon, authenticated
    USING (true);
