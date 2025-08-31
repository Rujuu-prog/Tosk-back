create table if not exists auth_session (
  id uuid primary key,
  user_id uuid not null references users(id) on delete cascade,
  current_rt_jti text not null,
  revoked_at timestamptz null,
  last_rotated_at timestamptz null,
  ip text null,
  user_agent text null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists ix_auth_session_user on auth_session(user_id);
