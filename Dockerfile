FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
VOLUME ["/app/uploads"]
EXPOSE 20935
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
