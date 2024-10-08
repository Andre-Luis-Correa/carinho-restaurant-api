package com.menumaster.restaurant.exception.type;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DuplicateKeyException extends RuntimeException{
    private String message;

    public DuplicateKeyException(String message) {
        super(message);
        this.message = message;
    }
}