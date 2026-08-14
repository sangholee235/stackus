import { useState } from "react";
import NicknameGate from "../components/NicknameGate";
import { createRoom } from "../services/api";
import { getSavedNickname } from "../services/nickname";

export default function RoomCreatePage() {
	const [nickname, setNickname] = useState<string | null>(() => getSavedNickname());
	const [roomName, setRoomName] = useState("");
	const [isCreating, setIsCreating] = useState(false);
	const [error, setError] = useState<string | null>(null);

	async function handleCreateRoom(e: React.FormEvent) {
		e.preventDefault();
		setIsCreating(true);
		setError(null);
		try {
			const room = await createRoom(roomName.trim());
			window.location.href = `/rooms/${room.roomId}`;
		} catch (e) {
			setError(e instanceof Error ? e.message : "방 생성에 실패했습니다.");
			setIsCreating(false);
		}
	}

	if (!nickname) {
		return (
			<NicknameGate
				title="3자리 암호"
				description="다른 사람들에게 보여질 닉네임을 정해주세요."
				submitLabel="계속하기"
				onSubmit={setNickname}
			/>
		);
	}

	return (
		<div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-slate-950 text-slate-100">
			<h1 className="text-3xl font-semibold">🔢 3자리 암호</h1>
			<p className="text-slate-400">
				링크를 공유하고, 아무 때나 들어와 다이얼을 돌려 숨겨진 암호를 다 같이 맞춰보세요.
			</p>
			<p className="text-xs text-slate-500">
				{nickname}님으로 입장합니다 ·{" "}
				<button type="button" onClick={() => setNickname(null)} className="underline">
					변경
				</button>
			</p>
			<form onSubmit={handleCreateRoom} className="flex flex-col items-center gap-3">
				<input
					value={roomName}
					onChange={(e) => setRoomName(e.target.value.slice(0, 30))}
					placeholder="방 이름 (순위표에 표시됩니다)"
					className="w-64 rounded-md border border-slate-700 bg-slate-900 px-4 py-2 text-center text-slate-100 outline-none focus:border-indigo-400"
				/>
				<button
					type="submit"
					disabled={isCreating}
					className="rounded-md bg-indigo-500 px-6 py-2 font-medium hover:bg-indigo-400 disabled:opacity-50"
				>
					{isCreating ? "방 만드는 중..." : "방 만들기"}
				</button>
			</form>
			{error && <p className="text-red-400">{error}</p>}
			<a href="/leaderboard" className="text-sm text-slate-400 underline hover:text-slate-200">
				🏆 전체 순위 보기
			</a>
		</div>
	);
}
