FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY config ./config

RUN mvn -f config/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

COPY --from=build /app/config/target/*.jar app.jar

EXPOSE 8888
USER root
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod,native"]
