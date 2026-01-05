package com.example.minibank2.exception;

public class TransferToSameAccountException extends RuntimeException {
    public TransferToSameAccountException(String message) {
        super(message);
    }
}
