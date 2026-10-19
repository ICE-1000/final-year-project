package com.inventory.exception;

public class InvalidRequestStatusTransitionException extends RuntimeException {
    public InvalidRequestStatusTransitionException(String message) {
        super(message);
    }
}
