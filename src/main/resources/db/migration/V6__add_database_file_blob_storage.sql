create table file_blob (
    id bigserial primary key,
    created_at timestamptz,
    updated_at timestamptz,
    file_asset_id bigint not null,
    content bytea not null,
    constraint uk_file_blob_file_asset_id unique (file_asset_id),
    constraint fk_file_blob_file_asset_id foreign key (file_asset_id) references file_asset (id)
);

create index idx_file_blob_file_asset_id on file_blob (file_asset_id);

create index if not exists idx_file_asset_owner_id on file_asset (owner_id);
create index if not exists idx_file_asset_asset_type on file_asset (asset_type);
create index if not exists idx_file_asset_related on file_asset (related_type, related_id);
create index if not exists idx_file_asset_storage_key on file_asset (storage_key);
