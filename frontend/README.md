# frontend

numbaseball(협동 숫자야구)의 웹 클라이언트. React + TypeScript + Vite + Tailwind.

프로젝트 전체 설명과 실행 방법은 저장소 루트의 [README](../README.md)를 참고할 것.

## 이 디렉터리만 따로 실행

백엔드가 `http://localhost:8080`에 떠 있어야 한다.

```
cp .env.example .env
npm install
npm run dev
```

## 구조

```
src/
  pages/      화면 단위 (방 만들기 / 방 / 결과 목록)
  game/       추측 입력과 기록 UI
  hooks/      useRoomSocket — WebSocket 연결, 재연결, 서버 메시지 → 상태 반영
  services/   REST(api.ts), WebSocket(socket.ts), 닉네임 보관
  types/      서버와 주고받는 메시지 타입
```

라우팅 라이브러리는 쓰지 않는다. `App.tsx`에서 `window.location.pathname`으로만 분기하므로,
`/rooms/:id` 같은 경로를 새로고침해도 동작하려면 서버가 없는 경로를 `index.html`로
넘겨줘야 한다 (`nginx.conf`의 `try_files` 참고).
