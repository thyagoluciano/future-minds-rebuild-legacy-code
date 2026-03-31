FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY domain/pom.xml domain/
COPY api/pom.xml api/
COPY infrastructure/pom.xml infrastructure/
COPY query/pom.xml query/
COPY command-app/pom.xml command-app/
COPY query-app/pom.xml query-app/
COPY consumer-app/pom.xml consumer-app/
RUN mvn dependency:go-offline -pl query-app -am -DskipTests
COPY domain/src domain/src
COPY api/src api/src
COPY infrastructure/src infrastructure/src
COPY query/src query/src
COPY query-app/src query-app/src
RUN mvn clean package -pl query-app -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/query-app/target/*.jar app.jar
EXPOSE 8180
ENTRYPOINT ["java", "-jar", "app.jar"]
