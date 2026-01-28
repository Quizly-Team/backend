#!/bin/bash
set -euo pipefail

IMAGE_PATH=$1
IMAGE_TAG=$2
FULL_IMAGE_NAME="${IMAGE_PATH}:${IMAGE_TAG}"

LOG_DIR="/var/log/quizly"
mkdir -p "$LOG_DIR"

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

docker run -d --name "$TARGET_NAME" -p "${TARGET_PORT}:8080" \
  -v /root/quizly-server/application.yml:/application.yml \
  -v "$LOG_DIR":/logs \
  --memory="1280m" \
  "$FULL_IMAGE_NAME"

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

echo "> Nginx 설정 변경 완료. 트래픽이 새 버전으로 전환됨"
echo "> 이전 버전의 신규 트래픽 차단 및 기존 요청 처리를 위해 10초 대기 중..."
sleep 10

if [ "$CURRENT_PORT" -eq 8080 ]; then
  echo "> 이전 버전(backend-blue 컨테이너) Graceful Shutdown 시작 (미완료 요청 최대 60초 대기)..."
  docker stop backend-blue 2>/dev/null || true
  docker rm backend-blue 2>/dev/null || true
else
  echo "> 이전 버전(backend-green 컨테이너) Graceful Shutdown 시작 (미완료 요청 최대 60초 대기)..."
  docker stop backend-green 2>/dev/null || true
  docker rm backend-green 2>/dev/null || true
fi

echo "> 구버전 이미지 정리"
docker images --filter "reference=${IMAGE_PATH}" --format '{{.ID}}\t{{.CreatedAt}}' \
  | sort -k2,2r \
  | cut -f1 \
  | tail -n +3 \
  | xargs -r -I {} sh -c 'docker rmi -f {} &>/dev/null || true'
docker image prune -f

echo "> 배포 완료!"
