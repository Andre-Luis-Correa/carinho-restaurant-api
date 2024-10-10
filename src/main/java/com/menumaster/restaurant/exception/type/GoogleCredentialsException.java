package com.menumaster.restaurant.exception.type;

public class GoogleCredentialsException extends RuntimeException{
    private String message;

    public GoogleCredentialsException(String message) {
        this.message = message;
    }
}
