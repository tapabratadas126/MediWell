-- Migration: Ensure patient_profiles and patient_allergies tables exist with RLS
-- Run this in Supabase SQL Editor if not already applied.

CREATE TABLE IF NOT EXISTS public.patient_profiles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name text,
    phone text,
    date_of_birth text,
    city text,
    allergies text[] DEFAULT '{}',
    medical_conditions text[] DEFAULT '{}',
    kidney_conditions text[] DEFAULT '{}',
    liver_conditions text[] DEFAULT '{}',
    other_medicines text[] DEFAULT '{}',
    consent_data_use boolean DEFAULT false,
    consent_share_professional boolean DEFAULT false,
    created_at timestamptz DEFAULT now(),
    updated_at timestamptz DEFAULT now()
);

ALTER TABLE public.patient_profiles ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public' AND tablename = 'patient_profiles' AND policyname = 'user_owns_profile'
    ) THEN
        CREATE POLICY "user_owns_profile"
            ON public.patient_profiles
            FOR ALL
            USING (auth.uid() = user_id)
            WITH CHECK (auth.uid() = user_id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.patient_allergies (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    allergen_name text NOT NULL,
    reaction text,
    severity text DEFAULT 'Not specified',
    created_at timestamptz DEFAULT now(),
    updated_at timestamptz DEFAULT now()
);

ALTER TABLE public.patient_allergies ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public' AND tablename = 'patient_allergies' AND policyname = 'user_owns_allergies'
    ) THEN
        CREATE POLICY "user_owns_allergies"
            ON public.patient_allergies
            FOR ALL
            USING (auth.uid() = user_id)
            WITH CHECK (auth.uid() = user_id);
    END IF;
END $$;
