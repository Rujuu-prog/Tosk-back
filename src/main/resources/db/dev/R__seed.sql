-- Repeatable seed data for local/dev/docker profile only
-- Moved under db/dev and enabled only in docker profile (see application-docker.yml)

do $$
declare
  v_user_admin uuid;
  v_user_alice uuid;
  v_team_core uuid;
  v_task1 uuid;
  v_comment1 uuid;
begin
  -- admin user
  select id into v_user_admin from users where lower(email)=lower('admin@tosk.local') and deleted_at is null;
  if v_user_admin is null then
    insert into users (email, username, display_name, password_hash)
    values ('admin@tosk.local','admin','Administrator', null)
    returning id into v_user_admin;
  end if;

  -- alice user
  select id into v_user_alice from users where lower(email)=lower('alice@tosk.local') and deleted_at is null;
  if v_user_alice is null then
    insert into users (email, username, display_name, password_hash)
    values ('alice@tosk.local','alice','Alice', null)
    returning id into v_user_alice;
  end if;

  -- core team (owned by admin)
  select id into v_team_core from teams where lower(name)=lower('Core Team') and deleted_at is null;
  if v_team_core is null then
    insert into teams (name, owner_id)
    values ('Core Team', v_user_admin)
    returning id into v_team_core;
  end if;

  -- memberships (admin leader, alice member)
  perform 1 from user_team where user_id=v_user_admin and team_id=v_team_core and deleted_at is null;
  if not found then
    insert into user_team (user_id, team_id, role, status)
    values (v_user_admin, v_team_core, 'leader', 'joined');
  end if;

  perform 1 from user_team where user_id=v_user_alice and team_id=v_team_core and deleted_at is null;
  if not found then
    insert into user_team (user_id, team_id, role, status)
    values (v_user_alice, v_team_core, 'member', 'joined');
  end if;

  -- sample task (team-visible)
  select id into v_task1 from tasks where title='Welcome Task' and author_id=v_user_admin and deleted_at is null;
  if v_task1 is null then
    insert into tasks (author_id, team_id, visibility, title, description, priority)
    values (v_user_admin, v_team_core, 'team', 'Welcome Task', 'This is the first task for the team.', 'medium')
    returning id into v_task1;
  end if;

  -- sample comment (by Alice)
  select id into v_comment1 from comments where task_id=v_task1 and author_id=v_user_alice and deleted_at is null;
  if v_comment1 is null then
    insert into comments (task_id, author_id, content)
    values (v_task1, v_user_alice, 'Glad to join!')
    returning id into v_comment1;
  end if;

  -- likes (admin likes the task; alice likes the comment)
  perform 1 from task_like where task_id=v_task1 and user_id=v_user_admin;
  if not found then
    insert into task_like (task_id, user_id) values (v_task1, v_user_admin);
  end if;

  perform 1 from comment_like where comment_id=v_comment1 and user_id=v_user_alice;
  if not found then
    insert into comment_like (comment_id, user_id) values (v_comment1, v_user_alice);
  end if;

  -- notification example
  perform 1 from notifications where recipient_user_id=v_user_admin and notif_type='generic';
  if not found then
    insert into notifications (recipient_user_id, notif_type, payload)
    values (v_user_admin, 'generic', jsonb_build_object('message','Seed data created'));
  end if;
end$$;

