package com.menumaster.restaurant.exception.type;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntityNotFoundException extends RuntimeException {

    private String message;

    public EntityNotFoundException(String className, String identifier){
        super();
        this.message = "Não foi possível encontrar " + className + " com identificador: " + identifier;
    }
}