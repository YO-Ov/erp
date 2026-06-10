package com.hwlee.erp.notification;

import com.hwlee.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인앱 알림 한 건 — 한 명의 수신자에게 보내는 한 줄의 통지.
 *
 * <p>역할 단위 통지(예: "재무팀 전체")는 서비스가 해당 역할 사용자 수만큼 이 레코드를 복제해 만든다
 * (수신자별 1행 → 각자 읽음 처리 독립). {@link #linkUrl} 은 클릭 시 이동할 화면 경로(딥링크)다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification")
public class Notification extends BaseEntity {

    /** 수신자 = AppUser.username (이메일). */
    @Column(name = "recipient_username", nullable = false, length = 64)
    private String recipientUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    /** 클릭 시 이동할 화면 경로(딥링크). 예: {@code /sd/sales-orders/4}. 없으면 null. */
    @Column(name = "link_url", length = 200)
    private String linkUrl;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public static Notification of(String recipientUsername, NotificationType type,
                                  String title, String message, String linkUrl) {
        if (recipientUsername == null || recipientUsername.isBlank())
            throw new IllegalArgumentException("recipientUsername 은 비어 있을 수 없다.");
        if (type == null) throw new IllegalArgumentException("type 은 null 일 수 없다.");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title 은 비어 있을 수 없다.");
        Notification n = new Notification();
        n.recipientUsername = recipientUsername;
        n.type = type;
        n.title = title;
        n.message = message == null ? "" : message;
        n.linkUrl = linkUrl;
        return n;
    }

    /** 읽음 처리 (멱등 — 이미 읽었으면 그대로). */
    public void markRead(LocalDateTime now) {
        if (!read) {
            this.read = true;
            this.readAt = now;
        }
    }
}
