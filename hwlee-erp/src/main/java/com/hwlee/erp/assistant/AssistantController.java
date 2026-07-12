package com.hwlee.erp.assistant;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

/**
 * AI 어시스턴트 프록시 — ERP 화면에서 입력한 자연어를 로컬 LLM 에이전트(erp-agent, Python/FastAPI)로
 * 넘기고 그 결과를 그대로 돌려준다.
 *
 * <p>비즈니스 판단·LLM 추출은 전적으로 에이전트가 하고, ERP 는 '인증된 통로(proxy)' 역할만 한다.
 * 흐름: 브라우저(챗봇) → 이 컨트롤러(/api/assistant/chat) → 에이전트(/chat) → (에이전트가) ERP REST API.
 *
 * <p>에이전트 위치는 {@code assistant.agent.base-url}(기본 http://localhost:8000). LLM 첫 호출은
 * 모델 로딩으로 수십 초 걸릴 수 있어 read 타임아웃을 넉넉히 둔다. 에이전트가 꺼져 있으면 503 으로 안내한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/assistant")
@PreAuthorize("isAuthenticated()") // 챗봇은 로그인한 전 부서 공용
public class AssistantController {

    private final RestClient agent;

    public AssistantController(
            @Value("${assistant.agent.base-url:http://localhost:8000}") String baseUrl,
            @Value("${assistant.agent.timeout-seconds:120}") long timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.agent = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /**
     * 자연어 한 줄(또는 확인 요청)을 에이전트로 프록시한다. 요청/응답 JSON 을 그대로 통과시킨다.
     * 프런트가 보내는 body: {@code {message}} (1차) 또는 {@code {intent, confirm:true}} (확인).
     */
    @PostMapping(value = "/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> body,
                                  Authentication authentication,
                                  HttpServletRequest request) {
        // 에이전트가 '이 사용자로서' ERP API 를 호출하도록 신원·권한·토큰을 함께 넘긴다.
        //  - erp_user  : "내가 상신한/나의" 같은 1인칭 조회 필터용
        //  - erp_roles : 참고용(실제 인가는 ERP 가 토큰으로 강제)
        //  - erp_token : 사용자 JWT — 에이전트가 ERP API 호출 시 Bearer 로 실어 RBAC 를 그대로 적용받는다
        Map<String, Object> payload = new HashMap<>(body);
        if (authentication != null) {
            payload.put("erp_user", authentication.getName());
            payload.put("erp_roles", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).toList());
        }
        String token = resolveToken(request);
        if (token != null) {
            payload.put("erp_token", token);
        }
        try {
            Map<?, ?> result = agent.post().uri("/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return ResponseEntity.ok(result);
        } catch (ResourceAccessException e) {
            // 에이전트 서버 미기동/연결 실패 — 프런트가 그대로 렌더할 수 있게 {type,lines} 모양을 맞춘다.
            log.warn("AI 에이전트 연결 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "type", "error",
                    "lines", List.of("🔌 AI 에이전트 서버에 연결할 수 없습니다. (에이전트가 실행 중인지 확인하세요)")));
        } catch (Exception e) {
            log.warn("AI 에이전트 호출 오류", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "type", "error",
                    "lines", List.of("⚠️ AI 처리 중 오류가 발생했습니다: " + e.getMessage())));
        }
    }

    /** 요청에서 JWT 를 뽑는다 — Authorization: Bearer 헤더 우선, 없으면 ACCESS_TOKEN 쿠키. */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("ACCESS_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
