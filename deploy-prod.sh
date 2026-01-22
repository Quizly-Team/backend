#!/bin/bash
set -euo pipefail

IMAGE_PATH=$1
IMAGE_TAG=$2
FULL_IMAGE_NAME="${IMAGE_PATH}:${IMAGE_TAG}"

echo "> 배포 시작: $FULL_IMAGE_NAME"

if [ ! -f /etc/nginx/conf.d/service-url.inc ]; then
    echo "set \$service_url http://127.0.0.1:8080;" | sudo tee /etc/nginx/conf.d/service-url.inc
fi

CURRENT_PORT=$(cat /etc/nginx/conf.d/service-url.inc | grep -o '[0-9]*' | tail -1)

if [ "$CURRENT_PORT" -eq 8080 ]; then
  TARGET_PORT=8081
  TARGET_NAME="backend-green"
else
  TARGET_PORT=8080
  TARGET_NAME="backend-blue"
fi

echo "> 타겟 포트: $TARGET_PORT"

docker pull $FULL_IMAGE_NAME
docker rm -f "$TARGET_NAME" 2>/dev/null || true

docker run -d --name $TARGET_NAME -p ${TARGET_PORT}:8080 \
  -v /root/quizly-server/application.yml:/application.yml \
  --memory="1280m" \
  $FULL_IMAGE_NAME

echo "> Health Check (http://localhost:${TARGET_PORT}/)..."
for RETRY in {1..5}; do
  RESPONSE_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${TARGET_PORT}/actuator/health || echo "000")

  if [ "$RESPONSE_CODE" -eq 200 ]; then
      echo "> Health Check 성공! (응답 코드: $RESPONSE_CODE)"
      break
  fi

  if [ $RETRY -eq 5 ]; then
      echo "> Health Check 실패. (마지막 응답 코드: $RESPONSE_CODE)"
      echo "> 컨테이너 로그:"
      docker logs $TARGET_NAME --tail 20
      docker stop $TARGET_NAME
      exit 1
  fi

  echo "> 대기 중... ($RETRY/5 - 응답 코드: $RESPONSE_CODE)"
  sleep 10
done

echo "set \$service_url http://127.0.0.1:${TARGET_PORT};" | sudo tee /etc/nginx/conf.d/service-url.inc
sudo service nginx reload

if [ "$CURRENT_PORT" -eq 8080 ]; then
  docker stop backend-blue 2>/dev/null || true
  docker rm backend-blue 2>/dev/null || true
else
  docker stop backend-green 2>/dev/null || true
  docker rm backend-green 2>/dev/null || true
fi
docker image prune -f

echo "> 배포 완료!"
