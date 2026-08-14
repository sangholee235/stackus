import { useEffect, useState } from "react";
import NicknameGate from "../components/NicknameGate";
import CodeDial from "../game/CodeDial";
import { useRoomSocket, type ConnectionStatus } from "../hooks/useRoomSocket";
import { createRoom, getRoom } from "../services/api";
import { getSavedNickname } from "../services/nickname";
import type { Direction } from "../types/code";
import type { RoomStatus } from "../types/room";

interface RoomPageProps {
	roomId: string;
}

const ROOM_STATUS_LABEL: Record<RoomStatus, string> = {
	WAITING: "대기 중",
	PLAYING: "진행 중",
	FINISHED: "풀림",
};

const ROOM_STATUS_DOT: Record<RoomStatus, string> = {
	WAITING: "bg-slate-400",
	PLAYING: "bg-emerald-400",
	FINISHED: "bg-amber-400",
};

function useRoomName(roomId: string): string | null {
	const [name, setName] = useState<string | null>(null);

	useEffect(() => {
		getRoom(roomId)
			.then((room) => setName(room.name))
			.catch(() => setName(null));
	}, [roomId]);

	return name;
}

function formatElapsed(seconds: number): string {
	if (seconds < 60) {
		return `${seconds}초`;
	}
	const minutes = Math.floor(seconds / 60);
	if (minutes < 60) {
		return `${minutes}분 ${seconds % 60}초`;
	}
	const hours = Math.floor(minutes / 60);
	return `${hours}시간 ${minutes % 60}분`;
}

export default function RoomPage({ roomId }: RoomPageProps) {
	const [nickname, setNickname] = useState<string | null>(() => getSavedNickname());

	if (!nickname) {
		return (
			<NicknameGate
				title="3자리 암호"
				description="이 방에서 다른 사람들에게 보여질 닉네임을 정해주세요."
				submitLabel="입장하기"
				onSubmit={setNickname}
			/>
		);
	}

	return <RoomGame roomId={roomId} nickname={nickname} />;
}

function RoomGame({ roomId, nickname }: { roomId: string; nickname: string }) {
	const { connectionStatus, roomStatus, guess, turnCount, gameOver, playerCount, lastAction, adjust } =
		useRoomSocket(roomId, nickname);
	const isFinished = roomStatus === "FINISHED";
	const locked = connectionStatus !== "open" || isFinished;
	const roomName = useRoomName(roomId);

	function handleAdjust(digitIndex: number, direction: Direction) {
		if (!locked) {
			adjust({ digitIndex, direction });
		}
	}

	return (
		<div className="flex min-h-screen flex-col items-center gap-4 bg-slate-950 p-6 text-slate-100">
			<div className="flex flex-col items-center gap-1">
				<div className="flex items-center gap-3">
					<h1 className="text-2xl font-semibold">{roomName ?? "3자리 암호"}</h1>
					<CopyLinkButton />
					<NewRoomButton />
				</div>
				<p className="text-xs text-slate-500">{nickname}님으로 참여 중</p>
			</div>

			<StatsBar
				roomStatus={roomStatus}
				playerCount={playerCount}
				turnCount={turnCount}
				connectionStatus={connectionStatus}
			/>

			<CodeDial guess={guess} locked={locked} onAdjust={handleAdjust} />

			<p className="h-6 text-sm text-slate-400">
				{isFinished
					? "암호가 풀렸습니다! 🎉"
					: "링크로 들어온 아무나, 아무 때나 화살표로 자리를 돌릴 수 있어요."}
			</p>

			{!isFinished && lastAction && (
				<p className="text-xs text-slate-500">
					방금 <span className="text-slate-300">{lastAction.nickname}</span>님이{" "}
					{lastAction.digitIndex + 1}번째 자리를 {lastAction.direction === "UP" ? "▲" : "▼"} 돌렸어요
				</p>
			)}

			{isFinished && gameOver && (
				<GameOverModal
					code={gameOver.code}
					turnCount={gameOver.turnCount}
					elapsedSeconds={gameOver.elapsedSeconds}
					solverNickname={gameOver.solverNickname}
				/>
			)}
		</div>
	);
}

function StatsBar({
	roomStatus,
	playerCount,
	turnCount,
	connectionStatus,
}: {
	roomStatus: RoomStatus;
	playerCount: number;
	turnCount: number;
	connectionStatus: ConnectionStatus;
}) {
	return (
		<div className="flex flex-wrap items-center justify-center gap-2">
			<StatBadge>
				<span className={`h-1.5 w-1.5 rounded-full ${ROOM_STATUS_DOT[roomStatus]}`} />
				{ROOM_STATUS_LABEL[roomStatus]}
			</StatBadge>
			<StatBadge>👥 {playerCount}명 접속 중</StatBadge>
			<StatBadge>🔁 {turnCount}턴</StatBadge>
			<StatBadge>
				<span
					className={`h-1.5 w-1.5 rounded-full ${connectionStatus === "open" ? "bg-emerald-400" : "bg-amber-400"}`}
				/>
				{connectionStatus === "open" ? "연결됨" : connectionStatus === "connecting" ? "연결 중" : "끊김"}
			</StatBadge>
		</div>
	);
}

function StatBadge({ children }: { children: React.ReactNode }) {
	return (
		<span className="flex items-center gap-1.5 rounded-full border border-slate-800 bg-slate-900 px-3 py-1 text-xs text-slate-300">
			{children}
		</span>
	);
}

/**
 * 지금 이 방의 결과를 기다리지 않고, 아무 때나 완전히 새로운 방(=새 암호)을 만들어
 * 그 링크로 바로 이동한다. 게임이 끝나야만 새 방을 만들 수 있던 걸(GameOverModal
 * 안에서만 제공하던 버튼) 상시 노출로 바꾼 것 — 지금 풀리고 있는 암호를 그대로 두고
 * 다른 사람들과 별도로 새 방을 시작하고 싶을 수도 있기 때문이다.
 */
function NewRoomButton() {
	const [isCreating, setIsCreating] = useState(false);

	async function handleCreate() {
		setIsCreating(true);
		try {
			const room = await createRoom();
			window.location.href = `/rooms/${room.roomId}`;
		} catch {
			setIsCreating(false);
		}
	}

	return (
		<button
			type="button"
			onClick={handleCreate}
			disabled={isCreating}
			className="rounded-md border border-slate-700 px-3 py-1 text-xs text-slate-300 hover:bg-slate-800 disabled:opacity-50"
		>
			{isCreating ? "만드는 중..." : "새 링크 만들기"}
		</button>
	);
}

function CopyLinkButton() {
	const [copied, setCopied] = useState(false);

	async function handleCopy() {
		await navigator.clipboard.writeText(window.location.href);
		setCopied(true);
		setTimeout(() => setCopied(false), 1500);
	}

	return (
		<button
			type="button"
			onClick={handleCopy}
			className="rounded-md border border-slate-700 px-3 py-1 text-xs text-slate-300 hover:bg-slate-800"
		>
			{copied ? "복사됨!" : "링크 복사"}
		</button>
	);
}

function GameOverModal({
	code,
	turnCount,
	elapsedSeconds,
	solverNickname,
}: {
	code: number[];
	turnCount: number;
	elapsedSeconds: number;
	solverNickname: string;
}) {
	const [isCreating, setIsCreating] = useState(false);

	async function handleNewGame() {
		setIsCreating(true);
		const room = await createRoom();
		window.location.href = `/rooms/${room.roomId}`;
	}

	return (
		<div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
			<div className="flex w-full max-w-sm flex-col items-center gap-3 rounded-lg border border-emerald-400 bg-slate-900 px-8 py-6 text-center shadow-xl">
				<p className="text-xl font-bold text-emerald-300">암호를 풀었습니다! 🎉</p>
				<p className="text-4xl font-bold tabular-nums tracking-widest">{code.join(" ")}</p>
				<p className="text-sm text-slate-400">
					<span className="font-semibold text-slate-200">{solverNickname}</span>님이 {turnCount}번째 턴에 완성
				</p>
				<p className="text-sm text-slate-400">걸린 시간: {formatElapsed(elapsedSeconds)}</p>
				<button
					type="button"
					onClick={handleNewGame}
					disabled={isCreating}
					className="mt-2 w-full rounded-md bg-indigo-500 px-6 py-2 font-medium hover:bg-indigo-400 disabled:opacity-50"
				>
					{isCreating ? "만드는 중..." : "새 암호 만들기"}
				</button>
				<a href="/leaderboard" className="text-sm text-slate-400 underline hover:text-slate-200">
					🏆 최근 결과 보기
				</a>
			</div>
		</div>
	);
}
