#!/bin/bash
cd /home/ubuntu/gdgoc-be-app

# 기존 컨테이너 중지 및 삭제
docker-compose down

# 사용되지 않는 컨테이너, 이미지, 네트워크, 볼륨 정리
docker system prune -af

# 불필요한 Docker 볼륨도 정리 (옵션)
docker volume prune -f

# 최신 이미지 가져오기
export $(grep -v '^#' .env | xargs)
docker pull ${DOCKER_HUB_USERNAME}/gdgoc-be-app:latest

# 컨테이너 실행
docker-compose --env-file .env up -d
