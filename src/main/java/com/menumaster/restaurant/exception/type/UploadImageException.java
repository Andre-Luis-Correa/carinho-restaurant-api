package com.menumaster.restaurant.exception.type;

public class UploadImageException extends RuntimeException {
    String message;

    public UploadImageException(String path, String image) {
        super();
        this.message = "Não foi possível salvar a imagem " + image + " no diretório " + path;
    }

    public UploadImageException(String message) {
        super();
        this.message = message;
    }
}
