import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

// StrictMode는 개발 모드에서 effect를 두 번 실행해 WebSocket을 순간적으로
// 두 개 열었다 닫아 접속자 수 등 실시간 상태가 잠깐 흔들리는 원인이 되어 뺐다.
createRoot(document.getElementById('root')!).render(<App />)
