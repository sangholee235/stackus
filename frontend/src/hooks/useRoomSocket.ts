import { useEffect, useRef, useState } from "react";
import { connectRoomSocket, sendGuess } from "../services/socket";
import type { GameOverBroadcast, GuessMessage, GuessRecord, RejectionReason } from "../types/baseball";
import { DEFAULT_DIGIT_COUNT } from "../types/baseball";
import type { RoomStatus } from "../types/room";

export type ConnectionStatus = "connecting" | "open" | "closed";

const RECONNECT_BASE_DELAY_MS = 1000;
const RECONNECT_MAX_DELAY_MS = 10000;

/**
 * 서버가 추측을 거절한 이유를 사용자가 읽을 수 있는 문장으로 바꾼다.
 * 왜 안 먹혔는지 모르는 상태가 제일 답답하므로 사유별로 구체적으로 알려준다.
 */
const REJECTION_MESSAGE: Record<RejectionReason, string> = {
	LENGTH: "세 자리를 모두 입력해주세요.",
	RANGE: "0부터 9까지의 숫자만 쓸 수 있어요.",
	DUPLICATE: "같은 숫자를 두 번 쓸 수 없어요.",
	ALREADY_TRIED: "이미 누군가 시도한 조합이에요. 기록을 확인해보세요.",
	GAME_FINISHED: "이미 정답을 맞힌 방이에요.",
	ROOM_NOT_FOUND: "이 방은 만료되었거나 존재하지 않아요. 새 링크를 만들어주세요.",
	SERVER_BUSY: "다른 사람이 동시에 제출하는 중이에요. 잠시 후 다시 시도해주세요.",
	MALFORMED: "잘못된 요청이에요.",
};

export function useRoomSocket(roomId: string, nickname: string) {
	const socketRef = useRef<WebSocket | null>(null);
	const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
	const reconnectAttemptRef = useRef(0);
	const unmountedRef = useRef(false);

	const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>("connecting");
	const [roomStatus, setRoomStatus] = useState<RoomStatus>("WAITING");
	const [guesses, setGuesses] = useState<GuessRecord[]>([]);
	const [digitCount, setDigitCount] = useState(DEFAULT_DIGIT_COUNT);
	const [gameOver, setGameOver] = useState<GameOverBroadcast | null>(null);
	const [playerCount, setPlayerCount] = useState(0);
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
							setGuesses(event.guesses);
							setDigitCount(event.digitCount);
							break;
						case "GUESS_ADDED":
							setGuesses((previous) => [...previous, event.guess]);
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
							setGuesses((previous) =>
								previous.some((guess) => guess.guessId === event.winningGuess.guessId)
									? previous
									: [...previous, event.winningGuess],
							);
							setGameOver(event);
							setNotice(null);
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

	function submitGuess(message: GuessMessage) {
		if (socketRef.current && connectionStatus === "open") {
			sendGuess(socketRef.current, message);
		}
	}

	return {
		connectionStatus,
		roomStatus,
		guesses,
		digitCount,
		gameOver,
		playerCount,
		notice,
		roomUnavailable,
		submitGuess,
	};
}
