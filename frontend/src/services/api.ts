import type { GameResult, LeaderboardEntry, Room } from "../types/room";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

async function request<T>(path: string, init?: RequestInit): Promise<T> {
	const response = await fetch(`${API_BASE_URL}${path}`, {
		...init,
		credentials: "include",
		headers: {
			"Content-Type": "application/json",
			...init?.headers,
		},
	});
	if (!response.ok) {
		throw new Error(`Request failed: ${response.status} ${response.statusText}`);
	}
	return response.json() as Promise<T>;
}

export function createRoom(name?: string): Promise<Room> {
	return request<Room>("/api/rooms", { method: "POST", body: JSON.stringify({ name }) });
}

export function getRoom(roomId: string): Promise<Room> {
	return request<Room>(`/api/rooms/${roomId}`);
}

export function getGameResult(roomId: string): Promise<GameResult> {
	return request<GameResult>(`/api/rooms/${roomId}/result`);
}

export function getLeaderboard(): Promise<LeaderboardEntry[]> {
	return request<LeaderboardEntry[]>("/api/leaderboard");
}
