-- 인앱 알림: 수신자별 1행. 역할 단위 통지는 서비스가 수신자 수만큼 복제 생성한다.

CREATE TABLE notification (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    recipient_username VARCHAR(64)  NOT NULL COMMENT '수신자 = app_user.username(이메일)',
    type               VARCHAR(40)  NOT NULL COMMENT 'PRODUCTION_CANCELLED / CREDIT_REQUEST_* 등',
    title              VARCHAR(200) NOT NULL,
    message            VARCHAR(500) NOT NULL,
    link_url           VARCHAR(200) NULL     COMMENT '클릭 시 이동할 화면 경로(딥링크)',
    is_read            BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at            DATETIME     NULL,
    created_at         DATETIME     NOT NULL,
    created_by         VARCHAR(64)  NOT NULL,
    updated_at         DATETIME     NOT NULL,
    updated_by         VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notification_recipient (recipient_username, is_read, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '인앱 알림';
