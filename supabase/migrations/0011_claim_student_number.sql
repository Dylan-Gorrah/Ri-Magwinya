-- 0011_claim_student_number.sql
--
-- Lets an account supply its student number exactly once.
--
-- Why this is a function rather than a column grant: `student_number` is the
-- only thing tying an account to a real person, and it is unique. If it were
-- simply updatable, anyone could rewrite theirs at any time and take a
-- number that belongs to somebody else. This sets it only when it is still
-- null, which is precisely the Google sign-in window and nothing else.
--
-- Changing a number after the fact stays a manual job for Dylan in the
-- dashboard, which is the right amount of friction for something that
-- decides who collects whose food.

create or replace function public.claim_student_number(p_number text)
returns public.profiles
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user    uuid := auth.uid();
    v_number  text := upper(trim(p_number));
    v_profile public.profiles;
begin
    if v_user is null then
        raise exception 'NOT_AUTHENTICATED';
    end if;

    if v_number is null or v_number = '' then
        raise exception 'STUDENT_NUMBER_REQUIRED';
    end if;

    select * into v_profile from public.profiles where id = v_user for update;

    if not found then
        raise exception 'NO_PROFILE';
    end if;

    -- Already set. Returning quietly when it is the same value keeps a
    -- retried request harmless.
    if v_profile.student_number is not null then
        if v_profile.student_number = v_number then
            return v_profile;
        end if;
        raise exception 'STUDENT_NUMBER_ALREADY_SET';
    end if;

    begin
        update public.profiles
        set student_number = v_number
        where id = v_user
        returning * into v_profile;
    exception when unique_violation then
        raise exception 'STUDENT_NUMBER_TAKEN';
    end;

    return v_profile;
end;
$$;

grant execute on function public.claim_student_number(text) to authenticated;
