import { useEffect, useRef, useState } from "react";

interface GuessInputProps {
	digitCount: number;
	disabled: boolean;
	onSubmit: (digits: number[]) => void;
}

/**
 * 자리별 한 칸짜리 입력. 숫자를 넣으면 자동으로 다음 칸으로 넘어가고, 지우면 앞 칸으로
 * 돌아간다. 서버가 최종 판정을 하지만, 명백히 규칙에 어긋나는 입력(중복 숫자 등)은
 * 여기서 미리 막아 헛된 왕복을 줄인다.
 */
export default function GuessInput({ digitCount, disabled, onSubmit }: GuessInputProps) {
	const [digits, setDigits] = useState<string[]>(() => Array(digitCount).fill(""));
	const inputsRef = useRef<(HTMLInputElement | null)[]>([]);

	useEffect(() => {
		setDigits(Array(digitCount).fill(""));
	}, [digitCount]);

	const filled = digits.every((digit) => digit !== "");
	const hasDuplicate = new Set(digits.filter(Boolean)).size !== digits.filter(Boolean).length;
	const canSubmit = !disabled && filled && !hasDuplicate;

	function setDigitAt(index: number, value: string) {
		const digit = value.replace(/\D/g, "").slice(-1);
		setDigits((previous) => {
			const next = [...previous];
			next[index] = digit;
			return next;
		});
		if (digit && index < digitCount - 1) {
			inputsRef.current[index + 1]?.focus();
		}
	}

	function handleKeyDown(index: number, event: React.KeyboardEvent<HTMLInputElement>) {
		if (event.key === "Backspace" && !digits[index] && index > 0) {
			inputsRef.current[index - 1]?.focus();
		}
		if (event.key === "Enter") {
			handleSubmit();
		}
	}

	function handleSubmit() {
		if (!canSubmit) {
			return;
		}
		onSubmit(digits.map(Number));
		setDigits(Array(digitCount).fill(""));
		inputsRef.current[0]?.focus();
	}

	return (
		<div className="flex flex-col items-center gap-3">
			<div className="flex gap-3">
				{digits.map((digit, index) => (
					<input
						key={index}
						ref={(element) => {
							inputsRef.current[index] = element;
						}}
						value={digit}
						onChange={(event) => setDigitAt(index, event.target.value)}
						onKeyDown={(event) => handleKeyDown(index, event)}
						disabled={disabled}
						inputMode="numeric"
						autoComplete="off"
						aria-label={`${index + 1}번째 자리`}
						className="h-20 w-14 rounded-md border border-slate-700 bg-slate-900 text-center text-4xl font-bold tabular-nums text-emerald-300 outline-none focus:border-emerald-400 disabled:opacity-40"
					/>
				))}
			</div>

			<button
				type="button"
				onClick={handleSubmit}
				disabled={!canSubmit}
				className="w-full max-w-[220px] rounded-md bg-emerald-500 px-6 py-2.5 font-medium text-slate-950 hover:bg-emerald-400 disabled:opacity-40"
			>
				추측 제출
			</button>

			<p className="h-4 text-xs text-rose-300">{hasDuplicate ? "같은 숫자를 두 번 쓸 수 없어요." : ""}</p>
		</div>
	);
}
