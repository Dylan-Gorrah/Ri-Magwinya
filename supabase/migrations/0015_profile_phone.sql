-- 0015_profile_phone.sql
--
-- A phone number on the profile, so staff can ring a student whose food is
-- going cold on the counter.
--
-- Nullable: every existing account has none, and nobody should be forced to
-- give one. The shape is checked loosely (7 to 20 characters of digits,
-- spaces, brackets, dashes and a leading +) because a strict South African
-- pattern would turn away a legitimate number written a way we did not
-- think of, and the app checks it more carefully before it ever gets here.

alter table public.profiles
    add column if not exists phone text
    check (phone is null or phone ~ '^[+]?[0-9 ()-]{7,20}$');

-- The student may edit it, like their name and language. The grants are
-- what actually stop them editing role or wallet_balance, so the new column
-- has to be named here or it would be read-only.
grant update (full_name, language, fcm_token, phone) on public.profiles to authenticated;
