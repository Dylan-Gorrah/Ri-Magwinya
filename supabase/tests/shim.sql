-- Local stand-ins for the parts of Supabase that are not plain Postgres.
-- Test harness only. Never applied to the real project.

-- Roles are cluster-wide, so this has to tolerate being run again.
do $shim$
begin
    if not exists (select 1 from pg_roles where rolname = 'anon') then
        create role anon nologin;
    end if;
    if not exists (select 1 from pg_roles where rolname = 'authenticated') then
        create role authenticated nologin;
    end if;
    if not exists (select 1 from pg_roles where rolname = 'service_role') then
        create role service_role nologin;
    end if;
end
$shim$;

create schema if not exists auth;

-- Supabase's auth.users, cut down to the columns handle_new_user touches.
create table auth.users (
    id                  uuid primary key default gen_random_uuid(),
    email               text unique,
    raw_user_meta_data  jsonb not null default '{}'::jsonb,
    created_at          timestamptz not null default now()
);

-- auth.uid() reads the JWT subject. Locally we drive it with a GUC so the
-- tests can say "now act as this person".
create or replace function auth.uid()
returns uuid
language sql
stable
as $$
    select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid;
$$;

create or replace function public.act_as(p_user uuid)
returns void
language sql
as $$
    select set_config('request.jwt.claim.sub', coalesce(p_user::text, ''), false);
$$;

-- Realtime publishes through this in hosted Supabase.
create publication supabase_realtime;
