insert into role (code, name, description, created_at, updated_at)
values
    ('ADMIN', 'Admin', 'Platform administrator', now(), now()),
    ('TEACHER', 'Teacher', 'Course teacher', now(), now()),
    ('STUDENT', 'Student', 'Course student', now(), now())
on conflict (code) do nothing;

insert into permission (code, name, description, created_at, updated_at)
values
    ('course:create', 'Create Course', 'Create courses', now(), now()),
    ('course:update', 'Update Course', 'Update courses', now(), now()),
    ('course:delete', 'Delete Course', 'Delete courses', now(), now()),
    ('course:publish', 'Publish Course', 'Publish courses', now(), now()),
    ('file:upload', 'Upload File', 'Upload files', now(), now()),
    ('file:delete', 'Delete File', 'Delete files', now(), now()),
    ('user:manage', 'Manage Users', 'Manage users', now(), now())
on conflict (code) do nothing;

insert into role_permissions (role_id, permission_id)
select role.id, permission.id
from role
cross join permission
where role.code = 'ADMIN'
on conflict on constraint uk_role_permissions_role_permission do nothing;

insert into role_permissions (role_id, permission_id)
select role.id, permission.id
from role
cross join permission
where role.code = 'TEACHER'
  and (
      permission.code like 'course:%'
      or permission.code like 'file:%'
  )
on conflict on constraint uk_role_permissions_role_permission do nothing;
