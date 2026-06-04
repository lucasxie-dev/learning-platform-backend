create table role (
    id bigserial primary key,
    created_at timestamptz,
    updated_at timestamptz,
    code varchar(50) not null,
    name varchar(100) not null,
    description varchar(500),
    constraint uk_role_code unique (code)
);

create table permission (
    id bigserial primary key,
    created_at timestamptz,
    updated_at timestamptz,
    code varchar(100) not null,
    name varchar(100) not null,
    description varchar(500),
    constraint uk_permission_code unique (code)
);

create table user_account (
    id bigserial primary key,
    created_at timestamptz,
    updated_at timestamptz,
    email varchar(255) not null,
    username varchar(100) not null,
    password_hash varchar(255) not null,
    display_name varchar(100),
    avatar_url varchar(1024),
    status varchar(30) not null,
    last_login_at timestamptz,
    constraint uk_user_account_email unique (email),
    constraint uk_user_account_username unique (username)
);

create table user_roles (
    user_id bigint not null,
    role_id bigint not null,
    constraint uk_user_roles_user_role unique (user_id, role_id),
    constraint fk_user_roles_user_id foreign key (user_id) references user_account (id),
    constraint fk_user_roles_role_id foreign key (role_id) references role (id)
);

create table role_permissions (
    role_id bigint not null,
    permission_id bigint not null,
    constraint uk_role_permissions_role_permission unique (role_id, permission_id),
    constraint fk_role_permissions_role_id foreign key (role_id) references role (id),
    constraint fk_role_permissions_permission_id foreign key (permission_id) references permission (id)
);

create index idx_user_account_status on user_account (status);
create index idx_user_roles_user_id on user_roles (user_id);
create index idx_user_roles_role_id on user_roles (role_id);
create index idx_role_permissions_role_id on role_permissions (role_id);
create index idx_role_permissions_permission_id on role_permissions (permission_id);
