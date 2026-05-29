create table if not exists meteomax_users (
    chat_id          text        primary key,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),
    userinfo         jsonb,
    favs             jsonb,
    last_latlon      jsonb,   -- [lat, lon]
    active           boolean not null default true
);

create table if not exists meteomax_subs (
    id           serial      primary key,
    created_at   timestamptz not null default now(),
    chat_id      text        not null,
    station_name text        not null,
    time_str     text        not null,
    days_of_week text        not null,
    active       boolean     not null default true
);

create index if not exists meteomax_subs_chat_id_idx on meteomax_subs (chat_id);
