export type Direction = "UP" | "DOWN";

/** 클라이언트가 서버에 보내는 조작. */
export interface AdjustMessage {
	digitIndex: number;
	direction: Direction;
}

/** 방 전체에 브로드캐스트되는 다이얼 상태 갱신. */
export interface GuessUpdateBroadcast {
	type: "GUESS_UPDATED";
	roomId: string;
	guess: number[];
	turnCount: number;
	actorNickname: string;
	digitIndex: number;
	direction: Direction;
}

/** 연결 직후 해당 세션에만 전송되는 현재 방 상태. secretCode는 포함되지 않는다. */
export interface RoomSyncMessage {
	type: "SYNC";
	roomId: string;
	status: string;
	guess: number[];
	turnCount: number;
}

export interface TurnRejectedMessage {
	type: "TURN_REJECTED";
	reason: "INVALID_ACTION" | "GAME_FINISHED" | "ROOM_NOT_FOUND" | "SERVER_BUSY";
	remainingMillis: number | null;
}

/** 암호가 풀린 순간의 결과. 이때만 정답(code)이 공개된다. */
export interface GameOverBroadcast {
	type: "GAME_OVER";
	roomId: string;
	code: number[];
	turnCount: number;
	elapsedSeconds: number;
	solverPlayerId: string;
	solverNickname: string;
	endedAt: string;
}

export interface PresenceMessage {
	type: "PRESENCE";
	roomId: string;
	playerCount: number;
}

export type ServerMessage =
	| GuessUpdateBroadcast
	| RoomSyncMessage
	| TurnRejectedMessage
	| GameOverBroadcast
	| PresenceMessage;

/** 백엔드 app.game.digit-count/digit-base 기본값과 일치해야 한다. */
export const DIGIT_COUNT = 3;
export const DIGIT_BASE = 10;
