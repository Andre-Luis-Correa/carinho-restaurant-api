package com.menumaster.restaurant.exception.type;

public class ExtractingJsonDataException extends RuntimeException {
    private String message;

    public ExtractingJsonDataException(String path) {
        super();
        this.message = "Erro ao extrair dados do json para o path: " + path;
    }
}
