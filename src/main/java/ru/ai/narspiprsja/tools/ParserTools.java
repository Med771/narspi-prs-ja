package ru.ai.narspiprsja.tools;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.ai.narspiprsja.config.ParserConfig;
import ru.ai.narspiprsja.model.Page;
import ru.ai.narspiprsja.model.Url;
import ru.ai.narspiprsja.property.ParserProperty;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParserTools {
    private final ParserProperty parserProperty;

    private static final Logger logger = LoggerFactory.getLogger(ParserTools.class);

    public Optional<Document> fetchPage(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(ParserConfig.USER_AGENT)
                    .referrer(ParserConfig.REFERRER)
                    .header("Accept-Language", ParserConfig.ACCEPT_LANGUAGE)
                    .ignoreHttpErrors(true)
                    .timeout(ParserConfig.TIMEOUT_MS)
                    .get();
            return Optional.of(doc);
        } catch (IOException e) {
            logger.warn("Ошибка при загрузке страницы: {}, {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    public List<Url> refactorPage(String part, int page) {
        List<Url> links = new ArrayList<>();
        String url = ParserConfig.BASE_URL.formatted(part) + ParserConfig.NEWS_PATH.formatted(
                parserProperty.getDate(), page);

        Optional<Document> docOpt = fetchPage(url);
        if (docOpt.isEmpty()) {
            logger.info("Нет HTML для {}", url);
            return links;
        }

        Element newsList = docOpt.get().selectFirst("div.news_list");
        if (newsList == null) {
            logger.info("Нет news_list в {}", url);
            return links;
        }

        Elements items = newsList.select("div.item_news");
        for (Element item : items) {
            Element linkTag = item.selectFirst("a.news-list_title");
            Element dateTag = item.selectFirst("div.news-list_date span");

            if (linkTag != null && dateTag != null) {
                String link = ParserConfig.BASE_URL.formatted(part) + linkTag.attr("href");

                LocalDateTime date;
                try {
                    date = LocalDateTime.parse(dateTag.text(), ParserConfig.FORMATTER);
                } catch (DateTimeParseException e) {
                    logger.warn("Не удалось распарсить дату '{}' на {}", dateTag.text(), url);
                    continue;
                }

                links.add(new Url(link, date));
            }
        }

        return links;
    }

    public List<Url> parseNewsPage(String part) {
        List<Url> news = new ArrayList<>();
        int page = 1;

        while (true) {
            List<Url> links = refactorPage(part, page);
            if (links.isEmpty()) break;

            news.addAll(links);
            page++;

            try {
                Thread.sleep(ParserConfig.SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Парсинг прерван", e);
                break;
            }
        }

        return news;
    }

    public Optional<Page> parseArticlePage(long postId, String url) {
        Optional<Document> docOpt = fetchPage(url);
        if (docOpt.isEmpty()) return Optional.empty();

        Element textBlock = docOpt.get().selectFirst("div.news_text");
        if (textBlock == null) return Optional.empty();

        List<String> chunks = Arrays.stream(textBlock.wholeText()
                        .replace("\r", "\n")
                        .split("\n\n"))
                .map(String::strip)
                .toList();

        StringBuilder newChunk = new StringBuilder();
        List<String> newChunks = new ArrayList<>();

        for (String chunk : chunks) {
            if (chunk.isEmpty()) {
                if (!newChunk.isEmpty()) {
                    newChunks.add(newChunk.toString());
                    newChunk.setLength(0);
                }
            } else {
                newChunk.append(chunk).append("\n");
            }
        }

        if (!newChunk.isEmpty()) {
            newChunks.add(newChunk.toString());
        }

        return Optional.of(new Page(postId, url, String.join("\n\n", newChunks)));
    }
}
