import { useEffect, useState } from "react";
import NicknameGate from "../components/NicknameGate";
import GuessHistory from "../game/GuessHistory";
import GuessInput from "../game/GuessInput";
import { useRoomSocket, type ConnectionStatus } from "../hooks/useRoomSocket";
import { createRoom, getRoom } from "../services/api";
import { getSavedNickname } from "../services/nickname";
import type { RoomStatus } from "../types/room";

interface RoomPageProps {
	roomId: string;
}

const ROOM_STATUS_LABEL: Record<RoomStatus, string> = {
	WAITING: "대기 중",
	PLAYING: "진행 중",
	FINISHED: "정답",
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

export function formatElapsed(seconds: number): string {
	if (seconds < 60) {
		return `${seconds}초`;
	}
	const minutes = Math.floor(seconds / 60);
	if (minutes < 60) {
		return `${minutes}분 ${seconds % 60}초`;
	}
	const hours = Math.floor(minutes / 60);
	if (hours < 24) {
		return `${hours}시간 ${minutes % 60}분`;
	}
	return `${Math.floor(hours / 24)}일 ${hours % 24}시간`;
}

export default function RoomPage({ roomId }: RoomPageProps) {
	const [nickname, setNickname] = useState<string | null>(() => getSavedNickname());

	if (!nickname) {
		return (
			<NicknameGate
				title="협동 숫자야구"
				description="이 방에서 다른 사람들에게 보여질 닉네임을 정해주세요."
				submitLabel="입장하기"
				onSubmit={setNickname}
			/>
		);
	}

	return <RoomGame roomId={roomId} nickname={nickname} />;
}

function RoomGame({ roomId, nickname }: { roomId: string; nickname: string }) {
	const {
		connectionStatus,
		roomStatus,
		guesses,
		digitCount,
		gameOver,
		playerCount,
		notice,
		roomUnavailable,
		submitGuess,
	} = useRoomSocket(roomId, nickname);
	const isFinished = roomStatus === "FINISHED";
	const locked = connectionStatus !== "open" || isFinished || roomUnavailable;
	const roomName = useRoomName(roomId);

	return (
		<div className="flex min-h-screen flex-col items-center gap-4 bg-slate-950 p-6 text-slate-100">
			<div className="flex flex-col items-center gap-1">
				<div className="flex items-center gap-3">
					<h1 className="text-2xl font-semibold">{roomName ?? "협동 숫자야구"}</h1>
					<CopyLinkButton />
					<NewRoomButton />
				</div>
				<p className="text-xs text-slate-500">{nickname}님으로 참여 중</p>
			</div>

			<StatsBar
				roomStatus={roomStatus}
				playerCount={playerCount}
				guessCount={guesses.length}
				connectionStatus={connectionStatus}
			/>

			{!isFinished && <RuleHint digitCount={digitCount} />}

			<GuessInput digitCount={digitCount} disabled={locked} onSubmit={(digits) => submitGuess({ digits })} />

			{notice && (
				<p className="rounded-md border border-amber-500/40 bg-amber-500/10 px-4 py-2 text-sm text-amber-200">
					{notice}
				</p>
			)}

			<GuessHistory guesses={guesses} />

			{isFinished && gameOver && (
				<GameOverModal
					code={gameOver.code}
					guessCount={gameOver.guessCount}
					elapsedSeconds={gameOver.elapsedSeconds}
					solverNickname={gameOver.solverNickname}
				/>
			)}
		</div>
	);
}

/**
 * 규칙 안내. 한 문단으로 늘어놓으면 줄바꿈이 제멋대로 걸려 읽기 나쁘므로,
 * 판정 기호는 줄 단위로 끊어서 눈에 바로 들어오게 배치한다.
 */
function RuleHint({ digitCount }: { digitCount: number }) {
	return (
		<div className="flex max-w-xs flex-col items-center gap-1.5 text-xs text-slate-500">
			<p>서로 다른 숫자 {digitCount}개를 맞혀보세요.</p>
			<div className="flex flex-wrap items-center justify-center gap-x-3 gap-y-1">
				<span>
					<span className="font-semibold text-emerald-300">S</span> 숫자와 자리가 맞음
				</span>
				<span>
					<span className="font-semibold text-amber-300">B</span> 숫자만 맞음
				</span>
				<span>
					<span className="font-semibold text-slate-400">아웃</span> 둘 다 없음
				</span>
			</div>
		</div>
	);
}

function StatsBar({
	roomStatus,
	playerCount,
	guessCount,
	connectionStatus,
}: {
	roomStatus: RoomStatus;
	playerCount: number;
	guessCount: number;
	connectionStatus: ConnectionStatus;
}) {
	return (
		<div className="flex flex-wrap items-center justify-center gap-2">
			<StatBadge>
				<span className={`h-1.5 w-1.5 rounded-full ${ROOM_STATUS_DOT[roomStatus]}`} />
				{ROOM_STATUS_LABEL[roomStatus]}
			</StatBadge>
			<StatBadge>👥 {playerCount}명 접속 중</StatBadge>
			<StatBadge>🎯 {guessCount}번째 시도</StatBadge>
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
 * 지금 이 방의 결과를 기다리지 않고, 아무 때나 완전히 새로운 방(=새 정답)을 만들어
 * 그 링크로 바로 이동한다.
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
	guessCount,
	elapsedSeconds,
	solverNickname,
}: {
	code: number[];
	guessCount: number;
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
				<p className="text-xl font-bold text-emerald-300">정답입니다! 🎉</p>
				<p className="text-4xl font-bold tabular-nums tracking-widest">{code.join(" ")}</p>
				<p className="text-sm text-slate-400">
					<span className="font-semibold text-slate-200">{solverNickname}</span>님이 {guessCount}번째 시도에 성공
				</p>
				<p className="text-sm text-slate-400">걸린 시간: {formatElapsed(elapsedSeconds)}</p>
				<button
					type="button"
					onClick={handleNewGame}
					disabled={isCreating}
					className="mt-2 w-full rounded-md bg-indigo-500 px-6 py-2 font-medium hover:bg-indigo-400 disabled:opacity-50"
				>
					{isCreating ? "만드는 중..." : "새 문제 만들기"}
				</button>
				<a href="/leaderboard" className="text-sm text-slate-400 underline hover:text-slate-200">
					🏆 최고 기록 보기
				</a>
			</div>
		</div>
	);
}
