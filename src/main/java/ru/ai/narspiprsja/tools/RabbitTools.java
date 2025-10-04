package ru.ai.narspiprsja.tools;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ai.narspiprsja.body.PageReq;
import ru.ai.narspiprsja.body.UrlsReq;
import ru.ai.narspiprsja.body.UrlsRes;
import ru.ai.narspiprsja.config.RabbitConfig;
import ru.ai.narspiprsja.model.Page;
import ru.ai.narspiprsja.model.Site;
import ru.ai.narspiprsja.model.Url;
import ru.ai.narspiprsja.property.ParserProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RabbitTools {
    private final ParserTools parserTools;
    private final ParserProperty parserProperty;

    private final RabbitConfig rabbitConfig;
    private final RabbitTemplate rabbitTemplate;

    private static final Logger logger = LoggerFactory.getLogger(RabbitTools.class);

    public Optional<UrlsRes> urlsSendAndReceive(UrlsReq req) {
        logger.info("[UUID: {}] Send urls size: {}", req.uuid(), req.data().size());

        try {
            UrlsRes res = rabbitTemplate.convertSendAndReceiveAsType(
                    rabbitConfig.getGtwExc(),
                    rabbitConfig.getUrlsReqRoutingKey(),
                    req,
                    new ParameterizedTypeReference<>() {}
            );

            if (res == null) {
                throw new RuntimeException("Response is null");
            }

            if (!req.uuid().equals(res.uuid())) {
                throw new RuntimeException("Response does not match uuid");
            }

            if (res.sites() == null || res.sites().isEmpty()) {
                throw new RuntimeException("Response answer is empty");
            }

            return Optional.of(res);
        }
        catch (Exception e) {
            logger.error("[UUID: {}] Urls response exception: {}", req.uuid(), e.getMessage());

            return Optional.empty();
        }
    }

    public void pageSend(List<Page> pages) {
        PageReq req = new PageReq(
                UUID.randomUUID(),
                pages
        );

        logger.info("[UUID: {}] Send pages size: {}", req.uuid(), req.pages().size());

        rabbitTemplate.convertAndSend(
                rabbitConfig.getGtwExc(),
                rabbitConfig.getPageReqRoutingKey(),
                req);
    }

    public void parseUrls(String part) {
        List<Url> urls = parserTools.parseNewsPage(part);
        if (urls.isEmpty()) { return; }

        UrlsReq req = new UrlsReq(
                UUID.randomUUID(),
                part,
                urls
        );

        Optional<UrlsRes> res = urlsSendAndReceive(req);

        if (res.isEmpty() || res.get().sites().isEmpty()) {return;}

        List<Page> pages = new ArrayList<>();

        for (Site site: res.get().sites()) {
            Optional<Page> page = parserTools.parseArticlePage(site.postId(), site.source());

            page.ifPresent(pages::add);

            if (pages.size() >= 20) {
                pageSend(pages);
                pages.clear();
            }
        }

        if (!pages.isEmpty()) {
            pageSend(pages);
        }
    }

    //@Scheduled(fixedDelay = 10000)
    public void task() {
        for (String part: parserProperty.getParts()) {
            parseUrls(part);
        }
    }
}
