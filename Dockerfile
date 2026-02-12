# syntax=docker/dockerfile:1

# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 루트 프로젝트 전체 복사
COPY . .

# wrapper가 있다면 wrapper 사용 권장
# 권한 부여는 로컬에서 이미 되어 있을 수도 있음
RUN chmod +x ./gradlew || true

# 테스트는 컨테이너 빌드에서 보통 스킵 (CI에서 이미 수행)
RUN ./gradlew clean bootJar -x test --no-daemon

# 산출물 이름이 가변적이면 가장 최신 jar를 app.jar로 복사
RUN cp "$(ls build/libs/*.jar | head -n 1)" build/libs/app.jar

# --- Runtime stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
