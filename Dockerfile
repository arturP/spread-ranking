FROM openjdk:21-slim-bullseye

COPY target/spread-ranking-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]