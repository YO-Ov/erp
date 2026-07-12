package com.hwlee.erp.assistant;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * AI 어시스턴트 채팅 화면. 로그인한 누구나 접근 가능(전 부서 공용).
 * 실제 처리는 {@link AssistantController} 가 로컬 LLM 에이전트로 프록시한다.
 */
@Controller
public class AssistantViewController {

    @GetMapping("/assistant")
    public String chat() {
        return "assistant/chat";
    }
}
