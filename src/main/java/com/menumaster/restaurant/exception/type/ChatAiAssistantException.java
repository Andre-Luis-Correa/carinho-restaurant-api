package com.menumaster.restaurant.exception.type;

public class ChatAiAssistantException extends RuntimeException {
    private String message;

    public ChatAiAssistantException() {
        super();
        this.message = "Não foi possível interpretar a mensagem do usuário com o auxílio da assistente";
    }
}
