# EC2 단일 서버 배포

이 구성은 Nginx가 HTTPS를 처리하고, Docker Compose가 Spring WAR(Tomcat 9)와 MySQL 8.4를 같은 EC2에서 실행하는 단일 서버 구성이다. 외부에는 Nginx의 80/443만 공개한다. Compose의 백엔드 포트는 `127.0.0.1:8080`에만 바인딩하므로 보안 그룹에서 8080을 열지 않는다.

## 사전 조건

- EC2에 Docker Engine과 Docker Compose plugin을 설치한다.
- Nginx와 TLS 인증서가 이미 설정되어 있어야 한다.
- GitHub에서 이 브랜치를 EC2로 가져온다.

```bash
sudo mkdir -p /opt/jaedaero/config
sudo chown -R ubuntu:ubuntu /opt/jaedaero
cd /opt/jaedaero
git clone https://github.com/BellongBellong/Jaedaero_backend.git app
cd app
git checkout chore/ec2-mysql-deployment
cp deploy/production.env.example .env
cp deploy/application-local.properties.example /opt/jaedaero/config/application-local.properties
chmod 600 .env /opt/jaedaero/config/application-local.properties
```

`.env`와 `/opt/jaedaero/config/application-local.properties`의 예시값을 실제 비밀값으로 교체한다. 두 파일은 Git에 커밋하지 않는다.

`MYSQL_PASSWORD`와 `DB_PASSWORD`는 같은 값이어야 하며, DB 주소는 반드시 `mysql`을 사용한다. 컨테이너 내부에서 `localhost`는 MySQL 컨테이너가 아니다.

## 최초 실행

```bash
docker compose -f docker-compose.prod.yml config
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
```

MySQL 데이터 볼륨이 비어 있을 때만 `jaedaero_db_v1.sql`, 운영 기준 시드, 휴가 혜택 시드, 2026-08-18 마이그레이션이 자동 실행된다. 기존 데이터가 있는 볼륨에서 이 초기화 SQL을 다시 실행하지 않는다.

## Nginx 프록시

인증서를 발급한 도메인에 다음 location 설정을 둔다. `server_name`과 인증서 경로는 Certbot이 생성한 값으로 유지한다.

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

적용 전후 검증:

```bash
sudo nginx -t
sudo systemctl reload nginx
curl -I http://127.0.0.1:8080
curl -I https://jaedaero.서버.한국
```

## 업데이트

```bash
cd /opt/jaedaero/app
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

데이터를 포함해 삭제하려는 경우가 아니라면 `docker compose down -v`를 실행하지 않는다. `-v`는 MySQL 데이터 볼륨까지 삭제한다.
