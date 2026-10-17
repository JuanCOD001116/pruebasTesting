package com.example.taskmanager.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {

    private int code;

    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String path;

    public ErrorResponse() {
    }

    public ErrorResponse(int code, String message, LocalDateTime timestamp, String path) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.path = path;
    }
}
