.PHONY: all clean build dev run docker-build docker-run outdated

all: build

clean:
	rm -rf target/

build:
	clojure -T:build uber

dev:
	clojure -M:dev:nrepl

run:
	java -jar target/meteomax-1.0.*-standalone.jar

docker-build:
	docker build -t meteomax .

docker-run:
	docker run -p 7937:7937 --env-file .env meteomax

install:
	clojure -P

lint:
	clj-kondo --lint src

test:
	clojure -M:dev -e "(require 'clojure.test) (clojure.test/run-all-tests)"

outdated:
	clojure -Sdeps '{:deps {com.github.liquidz/antq {:mvn/version "RELEASE"}}}' -M -m antq.core || true
