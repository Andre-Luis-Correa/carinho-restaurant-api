package com.menumaster.restaurant.exception.type;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidationException extends RuntimeException {
    private String message;

    public ValidationException(String field) {
        super();
        this.message = "Not possible to validade field: " + field;
    }
}