import LeaderboardPage from "./pages/LeaderboardPage";
import RoomCreatePage from "./pages/RoomCreatePage";
import RoomPage from "./pages/RoomPage";

// 별도 라우팅 라이브러리 없이 최소한의 경로 매칭만 처리한다.
// 방 목록/이동이 복잡해지면 이후 단계에서 react-router 등의 도입을 검토한다.
function App() {
	const path = window.location.pathname;
	const roomMatch = path.match(/^\/rooms\/([^/]+)$/);

	if (roomMatch) {
		return <RoomPage roomId={roomMatch[1]} />;
	}
	if (path === "/leaderboard") {
		return <LeaderboardPage />;
	}

	return <RoomCreatePage />;
}

export default App;
