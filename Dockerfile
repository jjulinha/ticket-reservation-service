FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
LABEL authors="Lorenzo"

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-alpine

WORKDIR /app

RUN apk add --no-cache tzdata
ENV TZ=America/Sao_Paulo

COPY --from=build /app/target/ticket-service*.jar ticket-service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "ticket-service.jar"]