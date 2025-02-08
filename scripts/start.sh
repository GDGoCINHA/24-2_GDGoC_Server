#!/bin/bash
echo "Spring Boot 애플리케이션 실행 중..."

# 애플리케이션 경로 이동
cd /home/ubuntu/gdgoc

# 기존 파일 삭제
rm -rf /home/ubuntu/gdgoc/*

# 환경 변수 로드 (.env 파일 사용)
export $(grep -v '^#' .env | xargs)

# S3에서 ZIP 파일 다운로드 (버킷 이름과 리전 설정을 사용)
echo "S3에서 ZIP 파일 다운로드 중..."
aws s3 cp s3://$S3_BUCKET_NAME/deploy.zip ./deploy.zip --region $AWS_REGION

# ZIP 파일 해제
echo "ZIP 파일 해제 중..."
unzip -o deploy.zip -d ./ # 기존 파일 덮어쓰기

# 다운로드 후 ZIP 파일 삭제
echo "ZIP 파일 삭제 중..."
rm -f deploy.zip

# Spring Boot 실행 (백그라운드 모드)
nohup java -jar build/libs/*.jar > /dev/null 2>&1 &

echo "Spring Boot 애플리케이션 실행 완료!"
