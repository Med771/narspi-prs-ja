package ru.ai.narspiprsja.body;

import ru.ai.narspiprsja.model.Site;

import java.util.List;
import java.util.UUID;

public record UrlsRes(
        UUID uuid,
        List<Site> sites
) {
}
