#!/bin/bash
# ITDA Language 프로덕션 배포 스크립트
# 사용법: bash deploy.sh [도메인명]
#
# 사전 요구사항:
#   - Docker + Docker Compose 설치
#   - 도메인 DNS가 서버 IP를 가리키고 있어야 함
#   - backend/.env에 프로덕션 설정 완료

set -e

DOMAIN=${1:-api.itda.kr}
echo "=== ITDA Language 배포 시작 ==="
echo "도메인: $DOMAIN"

# 1. 프로덕션 환경변수 확인
if [ ! -f ../backend/.env ]; then
    echo "ERROR: backend/.env 파일이 없습니다."
    echo "  .env.example을 복사하여 프로덕션 값을 설정해주세요."
    exit 1
fi

# 2. 초기 SSL 인증서 발급 (처음 한 번만)
if [ ! -d "/etc/letsencrypt/live/$DOMAIN" ]; then
    echo "--- SSL 인증서 발급 ---"
    # 임시로 HTTP만으로 Nginx 기동 (certbot 인증용)
    docker compose -f docker-compose.prod.yml up -d nginx

    docker compose -f docker-compose.prod.yml run --rm certbot certonly \
        --webroot --webroot-path=/var/www/certbot \
        --email admin@itda.kr \
        --agree-tos --no-eff-email \
        -d "$DOMAIN"

    docker compose -f docker-compose.prod.yml down
fi

# 3. 전체 서비스 빌드 + 기동
echo "--- 서비스 빌드 + 기동 ---"
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d

# 4. DB 마이그레이션 (또는 자동 테이블 생성)
echo "--- DB 초기화 ---"
docker compose -f docker-compose.prod.yml exec app python -m app.seed_data

# 5. 헬스 체크
echo "--- 헬스 체크 ---"
sleep 5
curl -sf "http://localhost:8000/health" && echo " OK" || echo " FAIL"

echo "=== 배포 완료 ==="
echo "https://$DOMAIN/docs 에서 API 문서를 확인하세요."
