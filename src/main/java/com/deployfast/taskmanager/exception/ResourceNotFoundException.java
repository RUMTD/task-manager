package com.deployfast.taskmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée lorsqu'une ressource n'est pas trouvée.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s avec l'id %d introuvable", resourceName, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
