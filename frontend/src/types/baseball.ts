/** 한 번의 추측과 그 판정 결과. */
export interface GuessRecord {
	guessId: string;
	playerId: string;
	nickname: string;
	digits: number[];
	/** 숫자와 자리가 모두 맞은 개수. */
	strikes: number;
	/** 숫자는 있지만 자리가 틀린 개수. */
	balls: number;
	guessedAt: string;
}

/** 클라이언트가 제출하는 추측. */
export interface GuessMessage {
	digits: number[];
}

export interface GuessAddedBroadcast {
	type: "GUESS_ADDED";
	roomId: string;
	guess: GuessRecord;
	guessCount: number;
}

/** 연결 직후 해당 세션에만 전송되는 현재 방 상태. 정답은 포함되지 않는다. */
export interface RoomSyncMessage {
	type: "SYNC";
	roomId: string;
	status: string;
	guesses: GuessRecord[];
	digitCount: number;
}

export type RejectionReason =
	| "LENGTH"
	| "RANGE"
	| "DUPLICATE"
	| "ALREADY_TRIED"
	| "GAME_FINISHED"
	| "ROOM_NOT_FOUND"
	| "SERVER_BUSY"
	| "MALFORMED";

export interface TurnRejectedMessage {
	type: "TURN_REJECTED";
	reason: RejectionReason;
	remainingMillis: number | null;
}

/** 정답을 맞힌 순간의 결과. 이때만 정답(code)이 공개된다. */
export interface GameOverBroadcast {
	type: "GAME_OVER";
	roomId: string;
	code: number[];
	winningGuess: GuessRecord;
	guessCount: number;
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
	| GuessAddedBroadcast
	| RoomSyncMessage
	| TurnRejectedMessage
	| GameOverBroadcast
	| PresenceMessage;

/** 백엔드 app.game.digit-count 기본값과 일치해야 한다. SYNC로 실제 값이 오면 그걸 쓴다. */
export const DEFAULT_DIGIT_COUNT = 3;
