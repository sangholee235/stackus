# stackus

링크 하나로 다 같이 모여 3자리 암호를 맞추는 실시간 협동(+방해) 게임.

동시 접속 여부와 무관하게 아무나 링크로 들어와 다이얼(각 자리 ▲/▼)을 돌릴 수 있고,
공유된 다이얼이 비밀 코드와 정확히 일치하는 순간 그 방은 끝난다 — 몇 턴/몇 분 만에
풀렸는지, 마지막으로 맞춘 사람이 누구인지가 함께 기록된다.

## 프로젝트 구조

```
frontend/   React + TypeScript + Vite + Tailwind
backend/    Spring Boot (Java 17) + Redis + MySQL
deploy/     메모리가 작은 서버(예: 1GB 미만)에 배포할 때 쓰는 경량 구성
            (MySQL 대신 파일 기반 H2, Vite 대신 nginx 정적 서빙 + API/WS 프록시)
docker-compose.yml
```

## 게임 규칙

- 서버가 방 생성 시 000~999 중 하나를 비밀 코드로 정한다 (아무에게도 보여주지 않는다)
- 공유 다이얼은 3자리, 각 자리를 `+1`/`-1`(0↔9 순환)로만 조작할 수 있다
- 쿨타임 없음 — 누구나 아무 때나 연달아 조작 가능
- 다이얼이 비밀 코드와 같아지는 순간 즉시 종료되고, 그 시점의 턴 수·경과 시간(방
  생성 시각 기준)·마지막으로 맞춘 사람이 기록에 남는다
- 서버가 판정을 전담한다 — 클라이언트는 서버가 보내는 상태를 그대로 그릴 뿐이라
  (물리 연산이나 기하학적 판정이 전혀 없다) 여러 명이 동시에 봐도 화면이 어긋나지 않는다

## 로컬 개발환경 실행

### 1. 환경변수 준비

```
cp .env.example .env
```

### 2. 전체 스택 기동 (Docker Compose)

```
docker compose up -d --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080

### 3. 인프라만 Docker로 띄우고 Backend/Frontend는 로컬에서 실행하는 방법

```
docker compose up -d mysql redis

cd backend
./gradlew bootRun

cd frontend
npm install
npm run dev
```

이 경우 `frontend/.env`, `backend`의 환경변수(`MYSQL_HOST=localhost` 등)를 로컬 기준으로 맞춰야 한다.
`application.yml`은 환경변수가 없으면 `localhost` 기본값을 사용하도록 되어 있어 별도 설정 없이도 로컬 인프라에 연결된다.

## 검증 방법

1. `curl -i -X POST http://localhost:8080/api/rooms` → roomId 발급 및 `Set-Cookie: playerId=...` 확인
2. `curl -i http://localhost:8080/api/rooms/{roomId}` → 방 조회 확인
3. 브라우저에서 `http://localhost:5173` 접속 → 방 생성 → `/rooms/{roomId}`로 이동
4. 같은 방 URL을 다른 탭/브라우저에서 열고 다이얼을 조작 → 양쪽에 실시간 반영되는지 확인

## 배포 (메모리가 작은 서버)

`deploy/` 아래 별도 `docker-compose.yml`을 쓴다. RAM이 1GB가 안 되는 서버에서는
**서버에서 직접 빌드하지 말 것** — Gradle/JDK 빌드가 메모리를 다 먹어 서버 전체가
응답 불능에 빠질 수 있다. 대신 이미지를 로컬(자원이 넉넉한 환경)에서 미리 빌드해
`docker save`로 저장한 뒤, 서버로 옮겨 `docker load` + `docker compose up`(빌드 없이)만
실행한다.

```
# 로컬에서
cd deploy
docker compose --env-file .env build
docker save stackus-backend:latest stackus-frontend:latest | gzip > images.tar.gz

# 서버로 전송 후
docker load < images.tar.gz
docker compose --env-file .env up -d
```

`deploy/.env`에 `PUBLIC_ORIGIN`(예: `http://1.2.3.4`)과 `PUBLIC_WS_ORIGIN`(예: `ws://1.2.3.4`)을
실제 서버 주소로 설정해야 한다.
