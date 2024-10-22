package com.menumaster.restaurant.exception.type;

public class AudioTranscriptionException extends RuntimeException {
    private String message;

    public AudioTranscriptionException() {
        super();
        this.message = "Não foi possível transcrever o áudio corretamente.";
    }
}
