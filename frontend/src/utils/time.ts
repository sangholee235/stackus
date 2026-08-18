/**
 * 경과 시간을 사람이 읽는 표현으로. 이 게임은 몇 초 만에 끝나기도 하고 며칠에 걸쳐
 * 이어지기도 해서, 단위를 폭넓게 커버해야 한다.
 */
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
