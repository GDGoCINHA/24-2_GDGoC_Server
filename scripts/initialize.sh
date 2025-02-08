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

#!/bin/bash
echo "🚀 S3에서 다운로드한 ZIP 파일 압축 해제 중..."
cd /home/ubuntu/gdgoc

# 기존 파일 삭제
rm -rf /home/ubuntu/gdgoc/*

# ZIP 파일 압축 해제
unzip /home/ubuntu/gdgoc/deploy.zip -d /home/ubuntu/gdgoc
chmod +x /home/ubuntu/gdgoc/scripts/start.sh

echo "✅ 압축 해제 완료!"

