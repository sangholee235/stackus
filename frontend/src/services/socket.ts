import type { GuessMessage, ServerMessage } from "../types/baseball";

const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL as string;

export interface RoomSocketHandlers {
	onMessage?: (event: ServerMessage) => void;
	onOpen?: () => void;
	onClose?: () => void;
}

/** 방 하나에 대한 WebSocket 연결. 재연결 로직은 useRoomSocket 훅에서 처리한다. */
export function connectRoomSocket(roomId: string, nickname: string, handlers: RoomSocketHandlers): WebSocket {
	const socket = new WebSocket(
		`${WS_BASE_URL}/ws/rooms/${roomId}?nickname=${encodeURIComponent(nickname)}`,
	);

	socket.onopen = () => handlers.onOpen?.();
	socket.onclose = () => handlers.onClose?.();
	socket.onmessage = (event) => {
		handlers.onMessage?.(JSON.parse(event.data) as ServerMessage);
	};

	return socket;
}

export function sendGuess(socket: WebSocket, message: GuessMessage): void {
	socket.send(JSON.stringify(message));
}
