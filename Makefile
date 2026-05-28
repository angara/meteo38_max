.PHONY: all clean build dev dev-env run docker-build docker-run install lint check-reflect test outdated


APP_NAME   = meteomax
VER_MAJOR  = 0
VER_MINOR  = 1
MAIN_CLASS = meteomax.main

export APP_NAME
export VER_MAJOR
export VER_MINOR
export MAIN_CLASS

all: build

# Build
build:
	clojure -T:build uberjar

clean:
	rm -rf target/

# Development
dev:
	@set -a; . ./.env; clojure -M:dev

dev-env:
	docker compose -f docker-compose.dev.yml up -d

dev-env-stop:
	docker compose -f docker-compose.dev.yml down

revproxy_host ?= vsp

rev-proxy:
	ssh -fNR 8005:localhost:8005 $(revproxy_host)

install:
	clojure -P

# Run
run:
	@set -a; . .env; java -jar target/meteomax-1.0.*-standalone.jar

# Docker
docker-build:
	docker build -t meteomax .

docker-run:
	docker run -p 7937:7937 --env-file .env meteomax

# Quality
lint:
	clj-kondo --lint src

check-reflect:
	clojure -M -e "(set! *warn-on-reflection* true) (require 'meteomax.app.webhook 'meteomax.app.dispatch 'meteomax.app.subs 'meteomax.app.sender 'meteomax.app.command 'meteomax.db.subscriptions 'meteomax.db.users 'meteomax.app.fmt 'meteomax.main 'meteomax.app.maxapi 'meteomax.lib.random 'meteomax.config 'meteomax.db.pg 'meteomax.metrics.reg 'meteomax.metrics.export 'meteomax.app.meteo-api 'meteomax.lib.envvar)"

test:
	clj -M:test

# Maintenance
outdated:
	clojure -Sdeps '{:deps {com.github.liquidz/antq {:mvn/version "RELEASE"}}}' -M -m antq.core || true
