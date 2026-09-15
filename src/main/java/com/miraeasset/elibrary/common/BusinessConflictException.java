package com.miraeasset.elibrary.common;

import lombok.Getter;

@Getter
public class BusinessConflictException extends RuntimeException {

    private final String code;

    public BusinessConflictException(String code, String message) {
        super(message);
        this.code = code;
    }
}
