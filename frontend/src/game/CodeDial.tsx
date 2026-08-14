import type { Direction } from "../types/code";

interface CodeDialProps {
	guess: number[];
	locked: boolean;
	onAdjust: (digitIndex: number, direction: Direction) => void;
}

/**
 * 3자리 다이얼. 물리 엔진이 전혀 없다 — 서버가 보내는 정수 배열(guess)을 그대로
 * 숫자로 그릴 뿐이라, 화면과 서버 상태가 어긋날 여지가 없다. 애니메이션은 숫자가
 * 바뀔 때 CSS transition으로 살짝 튀는 정도만 준다.
 */
export default function CodeDial({ guess, locked, onAdjust }: CodeDialProps) {
	return (
		<div className="flex gap-4">
			{guess.map((digit, index) => (
				<div key={index} className="flex flex-col items-center gap-2">
					<button
						type="button"
						disabled={locked}
						onClick={() => onAdjust(index, "UP")}
						className="flex h-9 w-14 items-center justify-center rounded-md border border-slate-700 bg-slate-900 text-lg text-slate-300 hover:bg-slate-800 disabled:opacity-30"
					>
						▲
					</button>
					<div className="flex h-20 w-14 items-center justify-center rounded-md border border-slate-700 bg-slate-900 text-4xl font-bold tabular-nums text-emerald-300 transition-transform">
						{digit}
					</div>
					<button
						type="button"
						disabled={locked}
						onClick={() => onAdjust(index, "DOWN")}
						className="flex h-9 w-14 items-center justify-center rounded-md border border-slate-700 bg-slate-900 text-lg text-slate-300 hover:bg-slate-800 disabled:opacity-30"
					>
						▼
					</button>
				</div>
			))}
		</div>
	);
}
