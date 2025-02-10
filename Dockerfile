FROM gradle:8.0.2-jdk17 AS build
WORKDIR /app/gdgoc

COPY gdgoc/ .
RUN ls -l
RUN chmod +x gradlew
RUN ./gradlew build -x test

FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=build /app/gdgoc/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
