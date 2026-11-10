FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build -x test --no-daemon
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
