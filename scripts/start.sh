#!/bin/bash
echo "Spring Boot 애플리케이션 실행 중..."

# 애플리케이션 경로 이동
cd /home/ubuntu/gdgoc || { echo "디렉토리 이동 실패"; exit 1; }

# 기존 파일 삭제
echo "기존 파일 삭제 중..."
rm -rf /home/ubuntu/gdgoc/*

# 환경 변수 로드 (.env 파일 사용)
if [ -f .env ]; then
    echo ".env 파일 로드 중..."
    export $(grep -v '^#' .env | xargs)
else
    echo ".env 파일이 없습니다. 환경 변수를 수동으로 설정하세요."
    exit 1
fi

# S3에서 ZIP 파일 다운로드 (버킷 이름과 리전 설정을 사용)
echo "S3에서 ZIP 파일 다운로드 중..."
aws s3 cp s3://$S3_BUCKET_NAME/deploy.zip ./deploy.zip --region $AWS_REGION

# 다운로드 성공 여부 확인
if [ ! -f "deploy.zip" ]; then
    echo "S3에서 ZIP 파일 다운로드 실패!"
    exit 1
fi

# ZIP 파일 해제
echo "ZIP 파일 해제 중..."
unzip -o deploy.zip -d ./

# 다운로드 후 ZIP 파일 삭제
echo "ZIP 파일 삭제 중..."
rm -f deploy.zip

# 실행할 JAR 파일 찾기
JAR_FILE=$(ls build/libs/*.jar 2>/dev/null | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "Spring Boot 실행 파일(JAR)이 없습니다!"
    exit 1
fi

# Spring Boot 실행 (백그라운드 모드)
echo "Spring Boot 애플리케이션 실행: $JAR_FILE"
nohup java -jar "$JAR_FILE" > /dev/null 2>&1 &

echo "Spring Boot 애플리케이션 실행 완료!"
