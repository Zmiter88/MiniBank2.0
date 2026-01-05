package com.example.minibank2.dto;

import java.time.LocalDateTime;

public class TransferResponse {

    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();

    public TransferResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
