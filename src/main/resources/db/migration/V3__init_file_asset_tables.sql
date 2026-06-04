create table file_asset (
    id bigserial primary key,
    created_at timestamptz,
    updated_at timestamptz,
    original_name varchar(255) not null,
    storage_key varchar(1024) not null,
    url varchar(1024),
    content_type varchar(100) not null,
    size_bytes bigint not null,
    asset_type varchar(50) not null,
    storage_provider varchar(30) not null,
    owner_id bigint not null,
    related_type varchar(100),
    related_id bigint,
    checksum varchar(128)
);

create index idx_file_asset_owner_id on file_asset (owner_id);
create index idx_file_asset_asset_type on file_asset (asset_type);
create index idx_file_asset_related on file_asset (related_type, related_id);
create index idx_file_asset_storage_provider on file_asset (storage_provider);
create index idx_file_asset_storage_key on file_asset (storage_key);
