-- Add optional token_version for global invalidation
alter table if exists users add column if not exists token_version int not null default 0;

