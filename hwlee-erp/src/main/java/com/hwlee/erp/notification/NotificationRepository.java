package com.hwlee.erp.notification;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientUsernameOrderByIdDesc(String recipientUsername, Pageable pageable);

    Page<Notification> findByRecipientUsernameAndReadFalseOrderByIdDesc(String recipientUsername, Pageable pageable);

    long countByRecipientUsernameAndReadFalse(String recipientUsername);

    List<Notification> findByRecipientUsernameAndReadFalse(String recipientUsername);
}
