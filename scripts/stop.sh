#!/bin/bash
echo "기존 애플리케이션 종료 중..."

# 기존 Spring Boot 애플리케이션 종료
PID=$(pgrep -f 'java -jar')
if [ -n "$PID" ]; then
  echo "실행 중인 프로세스 종료: $PID"
  kill -9 $PID
else
  echo "종료할 프로세스가 없습니다."
fi

echo "기존 애플리케이션 종료 완료!"
