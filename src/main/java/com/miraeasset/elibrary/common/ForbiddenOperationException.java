package com.miraeasset.elibrary.common;

import lombok.Getter;

@Getter
public class ForbiddenOperationException extends RuntimeException {

    private final String code;

    public ForbiddenOperationException(String code, String message) {
        super(message);
        this.code = code;
    }
}
