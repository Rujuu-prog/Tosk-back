-- Best practices: partial unique indexes for soft-delete, extra indexes,
-- and triggers to maintain tasks.like_count and tasks.comment_count

-- 1) Partial unique indexes (only enforce when deleted_at is null)
-- users: email, username case-insensitive uniques among non-deleted rows
drop index if exists ux_users_email_ci;
drop index if exists ux_users_username_ci;
create unique index if not exists ux_users_email_ci_active on users (lower(email)) where deleted_at is null;
create unique index if not exists ux_users_username_ci_active on users (lower(username)) where deleted_at is null;

-- teams: name unique (case-insensitive) among non-deleted rows
drop index if exists ux_teams_name_ci;
create unique index if not exists ux_teams_name_ci_active on teams (lower(name)) where deleted_at is null;

-- 2) Additional helpful indexes
create index if not exists ix_tasks_due_date on tasks(due_date) where deleted_at is null;
create index if not exists ix_tasks_priority on tasks(priority) where deleted_at is null;
create index if not exists ix_comments_author on comments(author_id) where deleted_at is null;

-- 3) Triggers to maintain like_count and comment_count on tasks

-- 3-1) comment_count maintenance
create or replace function trg_tasks_comment_count_inc()
returns trigger language plpgsql as $$
begin
  update tasks set comment_count = comment_count + 1, updated_at = now()
  where id = new.task_id;
  return new;
end$$;

create or replace function trg_tasks_comment_count_dec()
returns trigger language plpgsql as $$
begin
  update tasks set comment_count = greatest(comment_count - 1, 0), updated_at = now()
  where id = old.task_id;
  return old;
end$$;

drop trigger if exists trg_comments_after_insert_inc on comments;
create trigger trg_comments_after_insert_inc
  after insert on comments
  for each row execute function trg_tasks_comment_count_inc();

drop trigger if exists trg_comments_after_delete_dec on comments;
create trigger trg_comments_after_delete_dec
  after delete on comments
  for each row execute function trg_tasks_comment_count_dec();

-- 3-2) like_count maintenance (task_like only)
create or replace function trg_tasks_like_count_inc()
returns trigger language plpgsql as $$
begin
  update tasks set like_count = like_count + 1, updated_at = now()
  where id = new.task_id;
  return new;
end$$;

create or replace function trg_tasks_like_count_dec()
returns trigger language plpgsql as $$
begin
  update tasks set like_count = greatest(like_count - 1, 0), updated_at = now()
  where id = old.task_id;
  return old;
end$$;

drop trigger if exists trg_task_like_after_insert_inc on task_like;
create trigger trg_task_like_after_insert_inc
  after insert on task_like
  for each row execute function trg_tasks_like_count_inc();

drop trigger if exists trg_task_like_after_delete_dec on task_like;
create trigger trg_task_like_after_delete_dec
  after delete on task_like
  for each row execute function trg_tasks_like_count_dec();

