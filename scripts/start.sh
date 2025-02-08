#!/bin/bash
echo "Spring Boot 애플리케이션 실행 중..."

# 애플리케이션 경로 이동
cd /home/ubuntu/gdgoc

# Spring Boot 실행 (백그라운드 모드)
nohup java -jar build/libs/*.jar > /dev/null 2>&1 &

echo "Spring Boot 애플리케이션 실행 완료!"
