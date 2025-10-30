FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY discovery ./discovery

RUN mvn -f discovery/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

COPY --from=build /app/discovery/target/*.jar app.jar

EXPOSE 8761
USER root
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
