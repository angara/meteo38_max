-- :name get-user :? :1
-- :doc Get user by chat_id
SELECT * FROM users WHERE chat_id = :chat_id LIMIT 1

-- :name insert-user :! :n
-- :doc Insert new user
INSERT INTO users (chat_id, username, first_name, created_at)
VALUES (:chat_id, :username, :first_name, NOW())

-- :name update-user :! :n
-- :doc Update user information
UPDATE users SET username = :username, first_name = :first_name, updated_at = NOW()
WHERE chat_id = :chat_id

-- :name upsert-user :! :n
-- :doc Insert or update user
INSERT INTO users (chat_id, username, first_name, created_at, updated_at)
VALUES (:chat_id, :username, :first_name, NOW(), NOW())
ON CONFLICT (chat_id) DO UPDATE
SET username = EXCLUDED.username,
    first_name = EXCLUDED.first_name,
    updated_at = NOW()

-- :name get-user-preferences :?
-- :doc Get all preferences for a user
SELECT key, value FROM user_preferences WHERE chat_id = :chat_id

-- :name set-user-preference :! :n
-- :doc Set user preference
INSERT INTO user_preferences (chat_id, key, value)
VALUES (:chat_id, :key, :value)
ON CONFLICT (chat_id, key) DO UPDATE
SET value = EXCLUDED.value

-- :name delete-user-preference :! :n
-- :doc Delete user preference
DELETE FROM user_preferences WHERE chat_id = :chat_id AND key = :key

-- :name get-favorite-stations :?
-- :doc Get user's favorite stations
SELECT station_name FROM favorite_stations WHERE chat_id = :chat_id ORDER BY id

-- :name add-favorite-station :! :n
-- :doc Add station to favorites
INSERT INTO favorite_stations (chat_id, station_name)
VALUES (:chat_id, :station_name)
ON CONFLICT (chat_id, station_name) DO NOTHING

-- :name remove-favorite-station :! :n
-- :doc Remove station from favorites
DELETE FROM favorite_stations WHERE chat_id = :chat_id AND station_name = :station_name

-- :name get-subscriptions :?
-- :doc Get user's subscriptions
SELECT id, station_name, time_str, days_of_week, active
FROM subscriptions
WHERE chat_id = :chat_id
ORDER BY id

-- :name get-active-subscriptions :?
-- :doc Get all active subscriptions for sender
SELECT s.id, s.chat_id, s.station_name, s.time_str, s.days_of_week
FROM subscriptions s
WHERE s.active = true

-- :name get-subscription :? :1
-- :doc Get subscription by ID
SELECT * FROM subscriptions WHERE id = :id LIMIT 1

-- :name insert-subscription :! :1
-- :doc Create new subscription
INSERT INTO subscriptions (chat_id, station_name, time_str, days_of_week, active)
VALUES (:chat_id, :station_name, :time_str, :days_of_week, true)
RETURNING id

-- :name update-subscription :! :n
-- :doc Update subscription
UPDATE subscriptions
SET station_name = :station_name,
    time_str = :time_str,
    days_of_week = :days_of_week,
    active = :active
WHERE id = :id

-- :name delete-subscription :! :n
-- :doc Delete subscription
DELETE FROM subscriptions WHERE id = :id AND chat_id = :chat_id

-- :name get-user-location :? :1
-- :doc Get user's last known location
SELECT latitude, longitude, updated_at
FROM user_locations
WHERE chat_id = :chat_id
LIMIT 1

-- :name upsert-user-location :! :n
-- :doc Insert or update user location
INSERT INTO user_locations (chat_id, latitude, longitude, updated_at)
VALUES (:chat_id, :latitude, :longitude, NOW())
ON CONFLICT (chat_id) DO UPDATE
SET latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    updated_at = NOW()
