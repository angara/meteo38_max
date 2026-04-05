#!/bin/bash
set -e

echo "Starting MAX Weather Bot..."

# Check required environment variables
if [ -z "$MAX_API_TOKEN" ]; then
    echo "ERROR: MAX_API_TOKEN is not set"
    exit 1
fi

if [ -z "$DATABASE_URL" ]; then
    echo "ERROR: DATABASE_URL is not set"
    exit 1
fi

if [ -z "$METEO_API_AUTH" ]; then
    echo "ERROR: METEO_API_AUTH is not set"
    exit 1
fi

# Run database migrations if needed
echo "Running database migrations..."
# TODO: Add migration step

# Start the application
echo "Starting application..."
exec java -jar /app/maxbot.jar
