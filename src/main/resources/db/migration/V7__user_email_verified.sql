alter table if exists users add column if not exists email_verified boolean not null default false;

