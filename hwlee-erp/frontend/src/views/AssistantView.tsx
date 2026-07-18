import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { sendChat } from '../api/assistant'
import { errorMessage } from '../api/client'
import type { AssistantRequest, AssistantResponse } from '../types/api'

// 예시 칩 — 운영에서 실제로 동작하는 조회 질문 위주(챗봇 첫인상).
const EXAMPLES = [
  '내가 상신한 결재 어떻게 됐어?',
  '이번 달 수주 합계 알려줘',
  '고객 총 몇 곳이야?',
  '결재 반려된 것 있어?',
]

// 화면에 쌓이는 말풍선 한 개.
interface ChatMsg {
  id: number
  who: 'user' | 'bot'
  kind: 'plan' | 'result' | 'error' | 'typing' | null
  lines: string[]
  /** plan 일 때만 — [실행] 시 confirm 재전송할 intent. */
  intent?: unknown
  /** plan 실행/취소가 끝나면 버튼을 감춘다. */
  resolved?: boolean
}

export default function AssistantView() {
  const [messages, setMessages] = useState<ChatMsg[]>([])
  const [input, setInput] = useState('')
  const idRef = useRef(0)
  const logRef = useRef<HTMLDivElement>(null)

  const nextId = () => ++idRef.current

  function push(msg: Omit<ChatMsg, 'id'>): number {
    const id = nextId()
    setMessages((ms) => [...ms, { ...msg, id }])
    return id
  }
  function removeMsg(id: number) {
    setMessages((ms) => ms.filter((m) => m.id !== id))
  }
  function resolvePlan(id: number) {
    setMessages((ms) => ms.map((m) => (m.id === id ? { ...m, resolved: true } : m)))
  }

  // 새 말풍선이 쌓이면 맨 아래로.
  useEffect(() => {
    logRef.current?.scrollTo({ top: logRef.current.scrollHeight })
  }, [messages])

  const mutation = useMutation({
    mutationFn: (body: AssistantRequest) => sendChat(body),
  })

  // 에이전트 호출 + 응답 렌더. showUser=true 면 사용자 말풍선도 먼저 찍는다.
  async function send(body: AssistantRequest, showUser: boolean, userText?: string) {
    if (showUser && userText) push({ who: 'user', kind: null, lines: [userText] })
    const typingId = push({ who: 'bot', kind: 'typing', lines: ['⏳ 에이전트가 처리 중입니다…'] })

    try {
      const data = await mutation.mutateAsync(body)
      removeMsg(typingId)
      render(data)
    } catch (e) {
      removeMsg(typingId)
      // client 인터셉터가 503(에이전트 미기동)도 Error 로 정규화한다.
      push({ who: 'bot', kind: 'error', lines: ['🔌 ' + errorMessage(e)] })
    }
  }

  function render(data: AssistantResponse) {
    if (!data || !data.type) {
      push({ who: 'bot', kind: 'error', lines: ['빈 응답'] })
      return
    }
    switch (data.type) {
      case 'plan':
        // 쓰기 작업 미리보기 — 실행/취소 버튼을 단다.
        push({ who: 'bot', kind: 'plan', lines: data.summary || [], intent: data.intent })
        break
      case 'result':
        push({ who: 'bot', kind: 'result', lines: data.lines || [] })
        break
      case 'error':
        push({ who: 'bot', kind: 'error', lines: data.lines || ['오류'] })
        break
      default:
        push({ who: 'bot', kind: null, lines: data.lines || [] })
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    const text = input.trim()
    if (!text || mutation.isPending) return
    setInput('')
    send({ message: text }, true, text)
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    // Enter 전송, Shift+Enter 줄바꿈.
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      onSubmit(e)
    }
  }

  function onExample(text: string) {
    if (mutation.isPending) return
    send({ message: text }, true, text)
  }

  // plan 실행 — intent 를 confirm=true 로 재전송.
  function onPlanRun(msg: ChatMsg) {
    resolvePlan(msg.id)
    send({ intent: msg.intent, confirm: true }, false)
  }
  function onPlanCancel(msg: ChatMsg) {
    resolvePlan(msg.id)
    push({ who: 'bot', kind: null, lines: ['⏹️ 취소했습니다. (아무것도 생성되지 않음)'] })
  }

  return (
    <div className="container">
      <div className="page-head">
        <div>
          <h1>AI 어시스턴트</h1>
          <p className="muted" style={{ marginTop: 4 }}>
            자연어로 조회·요청하세요. 쓰기 작업은 실행 전에 미리보기로 확인합니다.
          </p>
        </div>
      </div>

      <div
        className="panel"
        ref={logRef}
        style={{ minHeight: 360, maxHeight: '60vh', overflowY: 'auto' }}
      >
        {messages.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '32px 8px' }}>
            <div style={{ fontSize: 36 }}>🤖</div>
            <p className="muted" style={{ marginTop: 8 }}>
              무엇을 도와드릴까요? 아래 예시를 눌러 시작해 보세요.
            </p>
            <div
              style={{
                display: 'flex',
                flexWrap: 'wrap',
                gap: 8,
                justifyContent: 'center',
                marginTop: 14,
              }}
            >
              {EXAMPLES.map((ex) => (
                <button key={ex} className="sm" type="button" onClick={() => onExample(ex)}>
                  {ex}
                </button>
              ))}
            </div>
          </div>
        ) : (
          messages.map((m) => <Bubble key={m.id} msg={m} onRun={onPlanRun} onCancel={onPlanCancel} />)
        )}
      </div>

      <form onSubmit={onSubmit} style={{ marginTop: 12 }}>
        <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKeyDown}
            rows={1}
            placeholder="예) 이번 달 수주 합계 알려줘  (Enter 전송 · Shift+Enter 줄바꿈)"
            style={{ flex: 1, resize: 'vertical', minHeight: 42 }}
          />
          <button className="primary" type="submit" disabled={mutation.isPending || !input.trim()}>
            {mutation.isPending ? '전송 중…' : '전송'}
          </button>
        </div>
      </form>
    </div>
  )
}

// 말풍선 하나. 사용자는 오른쪽, 봇은 왼쪽 정렬. lines 는 텍스트로 렌더돼 XSS 안전.
function Bubble({
  msg,
  onRun,
  onCancel,
}: {
  msg: ChatMsg
  onRun: (m: ChatMsg) => void
  onCancel: (m: ChatMsg) => void
}) {
  const isUser = msg.who === 'user'
  const toneBg =
    msg.kind === 'error'
      ? 'rgba(239,68,68,0.12)'
      : msg.kind === 'plan'
        ? 'rgba(245,158,11,0.12)'
        : isUser
          ? 'var(--accent, #4b6ef5)'
          : 'var(--bg-panel, rgba(148,163,184,0.12))'

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: isUser ? 'flex-end' : 'flex-start',
        margin: '8px 0',
      }}
    >
      <div
        style={{
          maxWidth: '78%',
          padding: '10px 14px',
          borderRadius: 12,
          background: toneBg,
          color: isUser ? '#fff' : 'inherit',
          opacity: msg.kind === 'typing' ? 0.7 : 1,
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
        }}
      >
        {msg.kind === 'plan' && (
          <div style={{ fontWeight: 700, marginBottom: 6 }}>📋 실행 계획 — 확인이 필요합니다</div>
        )}
        {msg.lines.map((line, i) => (
          <div key={i}>{line}</div>
        ))}
        {msg.kind === 'plan' && !msg.resolved && (
          <div className="actions" style={{ marginTop: 10 }}>
            <button className="sm primary" type="button" onClick={() => onRun(msg)}>
              실행
            </button>
            <button className="sm" type="button" onClick={() => onCancel(msg)}>
              취소
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
