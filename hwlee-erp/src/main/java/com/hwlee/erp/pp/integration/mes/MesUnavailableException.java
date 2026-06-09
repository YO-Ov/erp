package com.hwlee.erp.pp.integration.mes;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * MES 호출이 (재시도·서킷브레이커 후에도) 실패했을 때 던지는 예외.
 * 503 SERVICE_UNAVAILABLE 로 응답된다 — "MES 가 지금 응답하지 못함".
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class MesUnavailableException extends RuntimeException {
    public MesUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
