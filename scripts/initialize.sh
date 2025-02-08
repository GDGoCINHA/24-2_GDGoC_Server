#!/bin/bash
echo "Java 17 JDK 설치 확인 중..."

# Java 17이 설치되어 있는지 확인
if ! java -version 2>&1 | grep -q "17"; then
  echo "Java 17이 설치되지 않았습니다. 설치 시작..."
  sudo apt update -y
  sudo apt install -y openjdk-17-jdk
  echo "Java 17 JDK 설치 완료!"
else
  echo "Java 17 JDK가 이미 설치되어 있습니다."
fi
