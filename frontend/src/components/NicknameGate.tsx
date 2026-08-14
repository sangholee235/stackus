import { useState } from "react";
import { saveNickname } from "../services/nickname";

interface NicknameGateProps {
	title: string;
	description: string;
	submitLabel: string;
	onSubmit: (nickname: string) => void;
}

const NICKNAME_MAX_LENGTH = 20;

export default function NicknameGate({ title, description, submitLabel, onSubmit }: NicknameGateProps) {
	const [nickname, setNickname] = useState("");

	function handleSubmit(e: React.FormEvent) {
		e.preventDefault();
		const trimmed = nickname.trim();
		if (!trimmed) {
			return;
		}
		saveNickname(trimmed);
		onSubmit(trimmed);
	}

	return (
		<div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-slate-950 p-6 text-slate-100">
			<h1 className="text-2xl font-semibold">{title}</h1>
			<p className="text-sm text-slate-400">{description}</p>
			<form onSubmit={handleSubmit} className="flex flex-col items-center gap-3">
				<input
					autoFocus
					value={nickname}
					onChange={(e) => setNickname(e.target.value.slice(0, NICKNAME_MAX_LENGTH))}
					placeholder="닉네임을 입력하세요"
					className="w-56 rounded-md border border-slate-700 bg-slate-900 px-4 py-2 text-center text-slate-100 outline-none focus:border-indigo-400"
				/>
				<button
					type="submit"
					disabled={!nickname.trim()}
					className="rounded-md bg-indigo-500 px-6 py-2 font-medium hover:bg-indigo-400 disabled:opacity-50"
				>
					{submitLabel}
				</button>
			</form>
		</div>
	);
}
