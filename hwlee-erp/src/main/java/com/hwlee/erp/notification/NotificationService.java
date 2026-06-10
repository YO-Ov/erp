package com.hwlee.erp.notification;

import com.hwlee.erp.notification.dto.NotificationResponse;
import com.hwlee.erp.security.user.AppUser;
import com.hwlee.erp.security.user.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인앱 알림 서비스 — 발행(특정 사용자 / 역할 전체)과 조회·읽음 처리.
 *
 * <p>발행은 다른 모듈의 이벤트 리스너·서비스가 호출한다(생산취소·여신요청 등). 같은 트랜잭션에
 * 합류하므로, 알림을 만든 원천 작업이 롤백되면 알림도 함께 사라진다(유령 알림 방지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository repository;
    private final AppUserRepository appUserRepository;
    private final Clock clock;

    /** 특정 사용자 1명에게 알림. username 이 비면 조용히 무시(영업담당 미지정 등). */
    @Transactional
    public void notifyUser(String username, NotificationType type, String title, String message, String linkUrl) {
        if (username == null || username.isBlank()) return;
        repository.save(Notification.of(username, type, title, message, linkUrl));
        log.info("알림 발행 [{}] → {} : {}", type, username, title);
    }

    /** 해당 역할(예: FINANCE)을 가진 모든 사용자에게 각각 알림(수신자별 1행). */
    @Transactional
    public void notifyRole(String roleCode, NotificationType type, String title, String message, String linkUrl) {
        var users = appUserRepository.findByRoleCode(roleCode);
        for (AppUser u : users) {
            repository.save(Notification.of(u.getUsername(), type, title, message, linkUrl));
        }
        log.info("알림 발행 [{}] → 역할 {} {}명 : {}", type, roleCode, users.size(), title);
    }

    public Page<NotificationResponse> list(String username, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? repository.findByRecipientUsernameAndReadFalseOrderByIdDesc(username, pageable)
                : repository.findByRecipientUsernameOrderByIdDesc(username, pageable);
        return page.map(this::toResponse);
    }

    public long unreadCount(String username) {
        return repository.countByRecipientUsernameAndReadFalse(username);
    }

    /** 읽음 처리 — 본인 알림만(타인 것 접근 시 없는 것으로 취급). */
    @Transactional
    public void markRead(Long id, String username) {
        Notification n = repository.findById(id)
                .filter(x -> x.getRecipientUsername().equals(username))
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: id=" + id));
        n.markRead(LocalDateTime.now(clock));
    }

    @Transactional
    public void markAllRead(String username) {
        LocalDateTime now = LocalDateTime.now(clock);
        repository.findByRecipientUsernameAndReadFalse(username).forEach(n -> n.markRead(now));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getLinkUrl(), n.isRead(), n.getCreatedAt());
    }
}
