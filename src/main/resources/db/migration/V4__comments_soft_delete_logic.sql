-- Comments = logical delete, Likes = physical delete
-- Adjust comment_count for logical delete transitions and guard insert/delete cases

-- 1) Recreate helper functions to be conditional on deleted_at
create or replace function trg_tasks_comment_count_inc()
returns trigger language plpgsql as $$
begin
  -- increment only when the inserted row is active (not logically deleted)
  if new.deleted_at is null then
    update tasks set comment_count = comment_count + 1, updated_at = now()
    where id = new.task_id;
  end if;
  return new;
end$$;

create or replace function trg_tasks_comment_count_dec()
returns trigger language plpgsql as $$
begin
  -- decrement only when the deleted row was active
  if old.deleted_at is null then
    update tasks set comment_count = greatest(comment_count - 1, 0), updated_at = now()
    where id = old.task_id;
  end if;
  return old;
end$$;

-- Toggle function for logical delete/undelete
create or replace function trg_tasks_comment_count_toggle()
returns trigger language plpgsql as $$
begin
  -- undelete: old was deleted, new is active -> increment
  if old.deleted_at is not null and new.deleted_at is null then
    update tasks set comment_count = comment_count + 1, updated_at = now()
    where id = new.task_id;
  end if;
  -- logical delete: old was active, new is deleted -> decrement
  if old.deleted_at is null and new.deleted_at is not null then
    update tasks set comment_count = greatest(comment_count - 1, 0), updated_at = now()
    where id = new.task_id;
  end if;
  return new;
end$$;

-- 2) (Re)create triggers
drop trigger if exists trg_comments_after_insert_inc on comments;
create trigger trg_comments_after_insert_inc
  after insert on comments
  for each row execute function trg_tasks_comment_count_inc();

drop trigger if exists trg_comments_after_delete_dec on comments;
create trigger trg_comments_after_delete_dec
  after delete on comments
  for each row execute function trg_tasks_comment_count_dec();

drop trigger if exists trg_comments_after_update_toggle on comments;
create trigger trg_comments_after_update_toggle
  after update of deleted_at on comments
  for each row execute function trg_tasks_comment_count_toggle();

-- 3) Index for active comments by task (common query)
create index if not exists ix_comments_task_active on comments(task_id) where deleted_at is null;

