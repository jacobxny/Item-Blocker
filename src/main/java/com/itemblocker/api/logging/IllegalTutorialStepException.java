package com.itemblocker.api.logging;

public class IllegalTutorialStepException extends RuntimeException {
    public IllegalTutorialStepException(String message) {
        super(message);
    }

    public IllegalTutorialStepException(String message, Throwable reason) {
        super(message, reason);
    }
}
