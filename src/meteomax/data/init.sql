-- Database schema for MAX Weather Bot

CREATE TABLE IF NOT EXISTS users (
    chat_id TEXT PRIMARY KEY,
    username TEXT,
    first_name TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_preferences (
    chat_id TEXT REFERENCES users(chat_id) ON DELETE CASCADE,
    key TEXT NOT NULL,
    value TEXT NOT NULL,
    PRIMARY KEY (chat_id, key)
);

CREATE TABLE IF NOT EXISTS favorite_stations (
    id SERIAL PRIMARY KEY,
    chat_id TEXT REFERENCES users(chat_id) ON DELETE CASCADE,
    station_name TEXT NOT NULL,
    UNIQUE(chat_id, station_name)
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id SERIAL PRIMARY KEY,
    chat_id TEXT REFERENCES users(chat_id) ON DELETE CASCADE,
    station_name TEXT NOT NULL,
    time_str TEXT NOT NULL,
    days_of_week INTEGER NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_locations (
    chat_id TEXT PRIMARY KEY REFERENCES users(chat_id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_active ON subscriptions(active);
CREATE INDEX IF NOT EXISTS idx_subscriptions_chat_id ON subscriptions(chat_id);
CREATE INDEX IF NOT EXISTS idx_favorite_stations_chat_id ON favorite_stations(chat_id);
