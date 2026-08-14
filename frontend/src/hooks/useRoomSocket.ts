import { useEffect, useRef, useState } from "react";
import { connectRoomSocket, sendAdjust } from "../services/socket";
import type { AdjustMessage, Direction, GameOverBroadcast } from "../types/code";
import type { RoomStatus } from "../types/room";
import { DIGIT_COUNT } from "../types/code";

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
		adjust,
	};
}
