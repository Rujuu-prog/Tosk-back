-- Flyway initial migration
-- Keep minimal to validate DB connectivity and migration pipeline

create table if not exists app_migration_check (
    id bigint generated always as identity primary key,
    created_at timestamptz not null default now()
);

