export type RoomStatus = "WAITING" | "PLAYING" | "FINISHED";

export interface Room {
	roomId: string;
	name: string;
	status: RoomStatus;
	createdAt: string;
}

export interface GameResult {
	roomId: string;
	roomName: string;
	guessCount: number;
	elapsedSeconds: number;
	participantCount: number;
	solverNickname: string | null;
	endedAt: string;
}

export interface LeaderboardEntry {
	roomId: string;
	roomName: string;
	guessCount: number;
	elapsedSeconds: number;
	participantCount: number;
	solverNickname: string | null;
	endedAt: string;
}
