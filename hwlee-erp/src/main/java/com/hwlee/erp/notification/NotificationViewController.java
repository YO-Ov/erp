package com.hwlee.erp.notification;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 알림 인박스 화면 — 인증된 모든 사용자가 자기 알림 전체를 본다(역할 무관).
 */
@Controller
public class NotificationViewController {

    @GetMapping("/notifications")
    public String inbox() {
        return "notification/list";
    }
}
