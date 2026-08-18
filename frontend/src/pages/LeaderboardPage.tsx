import { useEffect, useState } from "react";
import { getLeaderboard } from "../services/api";
import type { LeaderboardEntry } from "../types/room";

const RANK_MEDALS = ["🥇", "🥈", "🥉"];

function formatElapsed(seconds: number): string {
	if (seconds < 60) {
		return `${seconds}초`;
	}
	const minutes = Math.floor(seconds / 60);
	if (minutes < 60) {
		return `${minutes}분`;
	}
	const hours = Math.floor(minutes / 60);
	return `${hours}시간 ${minutes % 60}분`;
}

export default function LeaderboardPage() {
	const [entries, setEntries] = useState<LeaderboardEntry[] | null>(null);
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		getLeaderboard()
			.then(setEntries)
			.catch((e) => setError(e instanceof Error ? e.message : "결과를 불러오지 못했습니다."));
	}, []);

	return (
		<div className="flex min-h-screen flex-col items-center gap-4 bg-slate-950 p-6 text-slate-100">
			<div className="flex w-full max-w-md items-center justify-between">
				<h1 className="text-2xl font-semibold">🏆 최근 결과</h1>
				<a href="/" className="text-sm text-slate-400 underline hover:text-slate-200">
					방 만들기
				</a>
			</div>
			<p className="text-sm text-slate-400">가장 적은 횟수로 정답을 맞힌 방 20개</p>

			{error && <p className="text-red-400">{error}</p>}
			{!entries && !error && <p className="text-slate-400">불러오는 중...</p>}
			{entries && entries.length === 0 && (
				<p className="text-slate-400">아직 정답이 나온 방이 없습니다. 첫 기록을 남겨보세요!</p>
			)}

			{entries && entries.length > 0 && (
				<ol className="w-full max-w-md space-y-2">
					{entries.map((entry, index) => (
						<li
							key={`${entry.roomId}-${entry.endedAt}`}
							className="flex items-center gap-3 rounded-md border border-slate-800 bg-slate-900 px-4 py-3"
						>
							<span className="w-8 text-center text-lg">{RANK_MEDALS[index] ?? `#${index + 1}`}</span>
							<div className="flex-1">
								<p className="font-semibold">
									{entry.roomName}
									<span className="ml-2 text-xs font-normal text-slate-500">
										{entry.guessCount}번 만에 · {formatElapsed(entry.elapsedSeconds)} · 참여 {entry.participantCount}명
									</span>
								</p>
								<p className="text-xs text-slate-500">
									{entry.solverNickname ? `맞힌 사람: ${entry.solverNickname} · ` : ""}
									{new Date(entry.endedAt).toLocaleString()}
								</p>
							</div>
						</li>
					))}
				</ol>
			)}
		</div>
	);
}
