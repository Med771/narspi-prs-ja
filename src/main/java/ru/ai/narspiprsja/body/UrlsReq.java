package ru.ai.narspiprsja.body;

import java.util.List;
import java.util.UUID;

import ru.ai.narspiprsja.model.Url;

public record UrlsReq(
        UUID uuid,
        String type,
        List<Url> data
) {
}
