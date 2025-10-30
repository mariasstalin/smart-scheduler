FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./pom.xml

COPY common ./common
COPY appointment ./appointment

RUN mvn -f pom.xml -N install

RUN mvn -f common/pom.xml -B -DskipTests clean install

RUN mvn -f appointment/pom.xml -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

COPY --from=build /app/appointment/target/*.jar app.jar

EXPOSE 8082
USER root
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
