#!/bin/bash
# shellcheck disable=SC2164
cd /home/ubuntu/gdgoc-be-app-dev

# Docker & Docker Compose가 설치되어 있는지 확인
if ! [ -x "$(command -v docker)" ]; then
  echo "Docker가 설치되어 있지 않습니다. 설치 중..."
  sudo apt update
  sudo apt install -y docker.io
  sudo systemctl start docker
  sudo systemctl enable docker
  echo "Docker 설치 완료"
fi

if ! [ -x "$(command -v docker-compose)" ]; then
  echo "Docker Compose가 설치되어 있지 않습니다. 설치 중..."
  sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
  sudo chmod +x /usr/local/bin/docker-compose
  echo "Docker Compose 설치 완료"
fi

# 기존 컨테이너 중지 및 삭제
docker-compose -f docker-compose-dev.yml down

# 최신 이미지 가져오기
# shellcheck disable=SC2046
export $(grep -v '^#' .env | xargs)
# shellcheck disable=SC2086
docker pull ${DOCKER_HUB_USERNAME}/gdgoc-be-app-dev:latest

# 컨테이너 실행
docker-compose -f docker-compose-dev.yml --env-file .env up -d

# 정리는 컨테이너가 뜬 뒤에 한다.
# 기동 전에 `prune -af` 를 돌리면 down 직후라 모든 이미지가 미사용으로 잡혀
# redis·dozzle 까지 전부 삭제되고, 매 배포마다 이미지를 통째로 다시 받게 된다.
# 여기서는 방금 교체된 구버전 앱 이미지(dangling)만 지워진다.
docker image prune -f