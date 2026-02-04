package com.myce.client.common;

import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class InternalResponseHandler {

    private InternalResponseHandler() {
    }

    public static <T> T requireOk(ResponseEntity<T> response, CustomErrorCode errorCode) {
        if (response == null || !response.getStatusCode().equals(HttpStatus.OK)) {
            throw new CustomException(errorCode);
        }
        T body = response.getBody();
        if (body == null) {
            throw new CustomException(CustomErrorCode.INTERNAL_SERVER_ERROR);
        }
        return body;
    }
}
