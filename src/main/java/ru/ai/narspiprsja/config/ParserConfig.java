package ru.ai.narspiprsja.config;

import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class ParserConfig {
    public static final String BASE_URL = "https://%s.cap.ru/press_center/news";
    public static final String NEWS_PATH = "?date_start=%s&page=%s";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm | dd.MM.yyyy");

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0 Safari/537.36";

    public static final String REFERRER = "https://www.google.com";
    public static final String ACCEPT_LANGUAGE = "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7";
    public static final int TIMEOUT_MS = 10_000;
    public static final int SLEEP_MS = 100;
}
