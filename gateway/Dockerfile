FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY gateway ./gateway

RUN mvn -f gateway/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

COPY --from=build /app/gateway/target/*.jar app.jar

EXPOSE 8761
USER root
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
