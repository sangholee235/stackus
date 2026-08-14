import { useEffect, useRef, useState } from "react";
import { connectRoomSocket, sendAdjust } from "../services/socket";
import type { AdjustMessage, Direction, GameOverBroadcast, TurnRejectedMessage } from "../types/code";
import type { RoomStatus } from "../types/room";
import { DIGIT_COUNT } from "../types/code";

/**
 * 서버가 조작을 거절한 이유를 사용자가 읽을 수 있는 문장으로 바꾼다.
 * 예전에는 이 메시지를 아예 무시해서, 눌러도 아무 일이 안 일어나는데 이유는
 * 알 수 없는 상태가 됐다.
 */
const REJECTION_MESSAGE: Record<TurnRejectedMessage["reason"], string> = {
	ROOM_NOT_FOUND: "이 방은 만료되었거나 존재하지 않아요. 새 링크를 만들어주세요.",
	GAME_FINISHED: "이미 풀린 암호예요.",
	INVALID_ACTION: "잘못된 조작이에요.",
	SERVER_BUSY: "다른 사람이 동시에 돌리는 중이에요. 잠시 후 다시 눌러주세요.",
};

export type ConnectionStatus = "connecting" | "open" | "closed";

const RECONNECT_BASE_DELAY_MS = 1000;
const RECONNECT_MAX_DELAY_MS = 10000;

export function useRoomSocket(roomId: string, nickname: string) {
	const socketRef = useRef<WebSocket | null>(null);
	const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
	const reconnectAttemptRef = useRef(0);
	const unmountedRef = useRef(false);

	const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>("connecting");
	const [roomStatus, setRoomStatus] = useState<RoomStatus>("WAITING");
	const [guess, setGuess] = useState<number[]>(() => Array(DIGIT_COUNT).fill(0));
	const [turnCount, setTurnCount] = useState(0);
	const [gameOver, setGameOver] = useState<GameOverBroadcast | null>(null);
	const [playerCount, setPlayerCount] = useState(0);
	const [lastAction, setLastAction] = useState<{ nickname: string; digitIndex: number; direction: Direction } | null>(
		null,
	);
	/** 서버가 조작을 거절했을 때 사용자에게 보여줄 안내. 방이 사라진 경우엔 계속 남긴다. */
	const [notice, setNotice] = useState<string | null>(null);
	const [roomUnavailable, setRoomUnavailable] = useState(false);

	useEffect(() => {
		unmountedRef.current = false;

		function connect() {
			setConnectionStatus("connecting");
			const socket = connectRoomSocket(roomId, nickname, {
				onOpen: () => {
					reconnectAttemptRef.current = 0;
					setConnectionStatus("open");
				},
				onClose: () => {
					setConnectionStatus("closed");
					if (!unmountedRef.current) {
						scheduleReconnect();
					}
				},
				onMessage: (event) => {
					switch (event.type) {
						case "SYNC":
							setRoomStatus(event.status as RoomStatus);
							setGuess(event.guess);
							setTurnCount(event.turnCount);
							break;
						case "GUESS_UPDATED":
							setGuess(event.guess);
							setTurnCount(event.turnCount);
							setLastAction({ nickname: event.actorNickname, digitIndex: event.digitIndex, direction: event.direction });
							setNotice(null); // 정상 반영됐으니 직전 경고는 지운다
							break;
						case "TURN_REJECTED":
							setNotice(REJECTION_MESSAGE[event.reason]);
							if (event.reason === "ROOM_NOT_FOUND") {
								setRoomUnavailable(true);
							}
							break;
						case "GAME_OVER":
							setRoomStatus("FINISHED");
							setGuess(event.code);
							setTurnCount(event.turnCount);
							setGameOver(event);
							break;
						case "PRESENCE":
							setPlayerCount(event.playerCount);
							break;
					}
				},
			});
			socketRef.current = socket;
		}

		function scheduleReconnect() {
			const delay = Math.min(
				RECONNECT_BASE_DELAY_MS * 2 ** reconnectAttemptRef.current,
				RECONNECT_MAX_DELAY_MS,
			);
			reconnectAttemptRef.current += 1;
			reconnectTimerRef.current = setTimeout(connect, delay);
		}

		connect();

		return () => {
			unmountedRef.current = true;
			if (reconnectTimerRef.current) {
				clearTimeout(reconnectTimerRef.current);
			}
			socketRef.current?.close();
			socketRef.current = null;
		};
	}, [roomId, nickname]);

	function adjust(message: AdjustMessage) {
		if (socketRef.current && connectionStatus === "open") {
			sendAdjust(socketRef.current, message);
		}
	}

	return {
		connectionStatus,
		roomStatus,
		guess,
		turnCount,
		gameOver,
		playerCount,
		lastAction,
		notice,
		roomUnavailable,
		adjust,
	};
}
