-- Add bio and avatar_url columns to users table
alter table if exists users add column if not exists bio text;
alter table if exists users add column if not exists avatar_url text;