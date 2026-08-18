import type { GuessRecord } from "../types/baseball";

interface GuessHistoryProps {
	guesses: GuessRecord[];
}

/**
 * 지금까지의 모든 추측 기록. 이게 이 게임의 핵심 화면이다 — 나중에 들어온 사람도
 * 이 목록만 읽으면 바로 추리에 합류할 수 있어야 하므로, 최신 시도가 위에 오도록
 * 뒤집어 보여주고 스트라이크/볼을 한눈에 구분되게 표시한다.
 */
export default function GuessHistory({ guesses }: GuessHistoryProps) {
	if (guesses.length === 0) {
		return (
			<p className="py-6 text-center text-sm text-slate-500">
				아직 아무도 시도하지 않았어요. 첫 추측을 남겨보세요.
			</p>
		);
	}

	return (
		<ol className="flex w-full max-w-sm flex-col gap-1.5">
			{[...guesses].reverse().map((guess, reverseIndex) => {
				const attemptNumber = guesses.length - reverseIndex;
				const isOut = guess.strikes === 0 && guess.balls === 0;
				return (
					<li
						key={guess.guessId}
						className="flex items-center gap-3 rounded-md border border-slate-800 bg-slate-900 px-3 py-2"
					>
						<span className="w-6 shrink-0 text-right text-xs text-slate-600">{attemptNumber}</span>

						<span className="font-bold tabular-nums tracking-[0.3em] text-slate-100">
							{guess.digits.join("")}
						</span>

						<span className="ml-auto flex items-center gap-1.5">
							{isOut ? (
								<Badge className="border-slate-600 text-slate-400">아웃</Badge>
							) : (
								<>
									{guess.strikes > 0 && (
										<Badge className="border-emerald-500/50 bg-emerald-500/10 text-emerald-300">
											{guess.strikes}S
										</Badge>
									)}
									{guess.balls > 0 && (
										<Badge className="border-amber-500/50 bg-amber-500/10 text-amber-300">
											{guess.balls}B
										</Badge>
									)}
								</>
							)}
						</span>

						<span className="w-16 shrink-0 truncate text-right text-xs text-slate-500" title={guess.nickname}>
							{guess.nickname}
						</span>
					</li>
				);
			})}
		</ol>
	);
}

function Badge({ children, className }: { children: React.ReactNode; className: string }) {
	return (
		<span className={`rounded border px-1.5 py-0.5 text-xs font-semibold tabular-nums ${className}`}>
			{children}
		</span>
	);
}
