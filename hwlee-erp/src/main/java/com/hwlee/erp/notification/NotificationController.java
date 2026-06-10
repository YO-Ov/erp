package com.hwlee.erp.notification;

import com.hwlee.erp.notification.dto.NotificationResponse;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 API — 현재 로그인 사용자({@link Principal})의 알림만 다룬다. 별도 역할 제한 없이
 * 인증된 누구나 자기 알림을 조회/읽음 처리한다.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public Page<NotificationResponse> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                           Pageable pageable, Principal principal) {
        return service.list(principal.getName(), unreadOnly, pageable);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Principal principal) {
        return Map.of("count", service.unreadCount(principal.getName()));
    }

    @PostMapping("/{id}/read")
    public void read(@PathVariable Long id, Principal principal) {
        service.markRead(id, principal.getName());
    }

    @PostMapping("/read-all")
    public void readAll(Principal principal) {
        service.markAllRead(principal.getName());
    }
}
