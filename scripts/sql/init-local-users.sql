-- Local/bootstrap user seed script.
-- Do not run this unchanged in production.
--
-- Initial password for all three users: ChangeMe123!
-- Change these passwords immediately after first login.

insert into user_account (
    email,
    username,
    password_hash,
    display_name,
    status,
    created_at,
    updated_at
)
values
    (
        'admin@example.com',
        'admin',
        '$2a$10$axLeZBjpH67qgdWsSTp1Oeg5tGHOJvQtCv7w2M1/Us/Idhwh1mEbG',
        'Initial Admin',
        'ACTIVE',
        now(),
        now()
    ),
    (
        'teacher@example.com',
        'teacher',
        '$2a$10$dy2DgvhhW9HO.U6KgBIJguTjy3QTgsoREcB8.KI7L4CuL5PL1/KMO',
        'Initial Teacher',
        'ACTIVE',
        now(),
        now()
    ),
    (
        'student@example.com',
        'student',
        '$2a$10$Z..PcQEcEaQS5cFQRpS8UuEIky6A1vpzvcF7gRU35AM86HHczhMEG',
        'Initial Student',
        'ACTIVE',
        now(),
        now()
    )
on conflict (email) do nothing;

insert into user_roles (user_id, role_id)
select user_account.id, role.id
from user_account
join role on role.code = 'ADMIN'
where user_account.email = 'admin@example.com'
on conflict on constraint uk_user_roles_user_role do nothing;

insert into user_roles (user_id, role_id)
select user_account.id, role.id
from user_account
join role on role.code = 'TEACHER'
where user_account.email = 'teacher@example.com'
on conflict on constraint uk_user_roles_user_role do nothing;

insert into user_roles (user_id, role_id)
select user_account.id, role.id
from user_account
join role on role.code = 'STUDENT'
where user_account.email = 'student@example.com'
on conflict on constraint uk_user_roles_user_role do nothing;
