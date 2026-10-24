package com.deploywise.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalyzeRequest(
        @NotBlank(message = "log must not be empty")
        @Size(max = 500_000, message = "log is too large (max 500000 characters)")
        String log) {
}
