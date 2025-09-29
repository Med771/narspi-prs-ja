package ru.ai.narspiprsja.model;

import java.time.LocalDateTime;

public record Url(
        String link,
        LocalDateTime date
) {
}
