FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache bash curl

WORKDIR /app

COPY target/*.jar /app/maxbot.jar
COPY run.sh /app/run.sh

RUN chmod +x /app/run.sh

EXPOSE 7937

ENV METRICS_BIND=0.0.0.0
ENV METRICS_PORT=7937

CMD ["/app/run.sh"]
