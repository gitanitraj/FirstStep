package org.firststep.backend.ai.service;

public class AiProviderNotConfiguredException extends RuntimeException {
    public AiProviderNotConfiguredException(String message) {
        super(message);
    }
}
