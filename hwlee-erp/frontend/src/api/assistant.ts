import client from './client'
import type { AssistantRequest, AssistantResponse } from '../types/api'

// AI 어시스턴트 프록시 (/api/assistant/chat).
// ERP 백엔드가 로그인 사용자의 JWT·역할을 실어 로컬 LLM 에이전트로 넘긴다.
// 에이전트가 그 사용자로서 ERP REST 를 호출하므로 RBAC 이 그대로 적용된다.
//
// ⚠️ LLM 첫 호출은 모델 로딩으로 수십 초 걸릴 수 있어 타임아웃을 넉넉히 준다
//    (기본 client 는 10초라 여기서 개별 override).
export async function sendChat(body: AssistantRequest): Promise<AssistantResponse> {
  const { data } = await client.post<AssistantResponse>('/assistant/chat', body, {
    timeout: 130_000,
  })
  return data
}
