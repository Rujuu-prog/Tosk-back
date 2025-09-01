-- Core domain schema based on ER: User/Team/UserTeam/Task/Comment/Like/Notification
-- PostgreSQL + UUID + audit columns (created_at/updated_at/deleted_at)

-- 1) Extensions
create extension if not exists pgcrypto; -- for gen_random_uuid()

-- 2) Helper: updated_at auto-update trigger
create or replace function trg_set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end$$;

-- 3) Enums
do $$ begin
  if not exists (select 1 from pg_type where typname = 'user_team_role') then
    create type user_team_role as enum ('leader','member');
  end if;
  if not exists (select 1 from pg_type where typname = 'user_team_status') then
    create type user_team_status as enum ('pending','joined','rejected');
  end if;
  if not exists (select 1 from pg_type where typname = 'task_visibility') then
    create type task_visibility as enum ('private','team','public');
  end if;
  if not exists (select 1 from pg_type where typname = 'task_priority') then
    create type task_priority as enum ('low','medium','high');
  end if;
  if not exists (select 1 from pg_type where typname = 'notification_type') then
    create type notification_type as enum (
      'join_request_approved','join_request_rejected',
      'task_assigned','task_commented','task_liked','comment_liked',
      'generic'
    );
  end if;
end $$;

-- 4) Tables

-- users
create table if not exists users (
  id uuid primary key default gen_random_uuid(),
  email text not null,
  username text not null,
  display_name text not null,
  password_hash text null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz null
);
-- uniqueness (case-insensitive)
create unique index if not exists ux_users_email_ci on users (lower(email));
create unique index if not exists ux_users_username_ci on users (lower(username));
create trigger trg_users_set_updated_at before update on users
  for each row execute function trg_set_updated_at();

-- teams
create table if not exists teams (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  owner_id uuid not null references users(id) on delete restrict,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz null
);
create index if not exists ix_teams_owner on teams(owner_id);
create unique index if not exists ux_teams_name_ci on teams (lower(name));
create trigger trg_teams_set_updated_at before update on teams
  for each row execute function trg_set_updated_at();

-- user_team (membership)
create table if not exists user_team (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  team_id uuid not null references teams(id) on delete cascade,
  role user_team_role not null default 'member',
  status user_team_status not null default 'joined',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz null,
  unique (user_id, team_id)
);
create index if not exists ix_user_team_team on user_team(team_id);
create index if not exists ix_user_team_user on user_team(user_id);
create trigger trg_user_team_set_updated_at before update on user_team
  for each row execute function trg_set_updated_at();

-- tasks
create table if not exists tasks (
  id uuid primary key default gen_random_uuid(),
  author_id uuid not null references users(id) on delete restrict,
  team_id uuid null references teams(id) on delete cascade,
  visibility task_visibility not null default 'private',
  title text not null,
  description text null,
  due_date date null,
  priority task_priority not null default 'medium',
  like_count int not null default 0,
  comment_count int not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz null,
  constraint chk_tasks_team_visibility check (
    visibility <> 'team' or team_id is not null
  )
);
create index if not exists ix_tasks_author on tasks(author_id);
create index if not exists ix_tasks_team on tasks(team_id);
create index if not exists ix_tasks_visibility on tasks(visibility);
create trigger trg_tasks_set_updated_at before update on tasks
  for each row execute function trg_set_updated_at();

-- comments (threaded)
create table if not exists comments (
  id uuid primary key default gen_random_uuid(),
  task_id uuid not null references tasks(id) on delete cascade,
  author_id uuid not null references users(id) on delete restrict,
  parent_comment_id uuid null references comments(id) on delete cascade,
  content text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz null
);
create index if not exists ix_comments_task on comments(task_id);
create index if not exists ix_comments_parent on comments(parent_comment_id);
create trigger trg_comments_set_updated_at before update on comments
  for each row execute function trg_set_updated_at();

-- likes (split into 2 tables for strict FK)
create table if not exists task_like (
  id uuid primary key default gen_random_uuid(),
  task_id uuid not null references tasks(id) on delete cascade,
  user_id uuid not null references users(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (task_id, user_id)
);
create index if not exists ix_task_like_user on task_like(user_id);

create table if not exists comment_like (
  id uuid primary key default gen_random_uuid(),
  comment_id uuid not null references comments(id) on delete cascade,
  user_id uuid not null references users(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (comment_id, user_id)
);
create index if not exists ix_comment_like_user on comment_like(user_id);

-- notifications
create table if not exists notifications (
  id uuid primary key default gen_random_uuid(),
  recipient_user_id uuid not null references users(id) on delete cascade,
  notif_type notification_type not null default 'generic',
  payload jsonb not null default '{}'::jsonb,
  read_at timestamptz null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz null
);
create index if not exists ix_notifications_user on notifications(recipient_user_id);
create index if not exists ix_notifications_unread on notifications(recipient_user_id) where read_at is null;
create trigger trg_notifications_set_updated_at before update on notifications
  for each row execute function trg_set_updated_at();
