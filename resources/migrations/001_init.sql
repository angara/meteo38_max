create table if not exists users (
    user_id          text        primary key,
    chat_id          text,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),
    username         text,
    last_send_at     timestamptz,
    last_send_status text,
    favs             text[]      not null default '{}',
    location         jsonb[],
    info             jsonb
);

create table if not exists subs (
    id           serial      primary key,
    user_id      text        not null,
    chat_id      text        not null,
    station_name text        not null,
    time_str     text        not null,
    days_of_week text        not null,
    active       boolean     not null default true,
    created_at   timestamptz not null default now()
);

create index if not exists subs_user_id_idx on subs (user_id);
create index if not exists subs_active_idx  on subs (active);
