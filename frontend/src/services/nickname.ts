const STORAGE_KEY = "numbaseball_nickname";

export function getSavedNickname(): string | null {
	return localStorage.getItem(STORAGE_KEY);
}

export function saveNickname(nickname: string): void {
	localStorage.setItem(STORAGE_KEY, nickname);
}
