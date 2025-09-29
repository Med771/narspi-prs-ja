package ru.ai.narspiprsja.body;

import ru.ai.narspiprsja.model.Page;

import java.util.List;
import java.util.UUID;

public record PageReq(
        UUID uuid,
        List<Page> pages
) {
}
