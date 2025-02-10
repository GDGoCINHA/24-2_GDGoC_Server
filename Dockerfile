FROM gradle:8.0.2-jdk17 AS build
WORKDIR /app

COPY . .
RUN chmod +x gradlew
RUN ./gradlew build

FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=build /app/gdgoc/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
