package ru.ai.narspiprsja.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class OpenConfig {

    @Value("${parse.abbreviations-file}")
    private Resource abbreviationsFile;

    private final Set<String> abbrs = new HashSet<>();

    // Цепочка инициалов: "А. Б." или "А. Б. В."
    private static final Pattern INITIALS_SEQUENCE =
            Pattern.compile("\\b(?:[А-ЯЁ]\\.)(?:\\s*[А-ЯЁ]\\.){1,}");

    // Число с точкой (перечисления) — не трогать десятичные дроби
    private static final Pattern NUMBER_DOT_PATTERN =
            Pattern.compile("\\b(\\d+)\\.(?!\\d)");

    // Одиночная буква (не инициалы)
    private static final Pattern SINGLE_LETTER_ABBR =
            Pattern.compile("\\b([А-ЯЁа-яё])\\.(?!\\s*[А-ЯЁа-яё]\\.)");

    // 2–3 буквенные аббревиатуры
    private static final Pattern TWO_OR_THREE_LETTER_ABBR =
            Pattern.compile("\\b([А-ЯЁа-яё]{2,3})\\.");

    @PostConstruct
    public void loadAbbreviations() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                abbreviationsFile.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) abbrs.add(line);
            }
        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить список сокращений", e);
        }
    }

    // Утилита: безопасное replaceAll с функцией-генератором замены (Java 8+)
    private String regexReplaceAll(String input, Pattern p, Function<MatchResult, String> replacer) {
        Matcher m = p.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            MatchResult mr = m.toMatchResult();
            String repl = replacer.apply(mr);
            m.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Маскируем сокращения, чтобы SentenceDetector не ломал предложения.
     * Цель: заменить точку в конструкциях, где точка — НЕ конец предложения,
     * на маркер "<DOT>" (и объединить инициалы в единый токен).
     */
    public String mask(String text) {
        String result = text;

        if (result == null || result.isEmpty()) return result;

        // Сначала специальные конструкции, чтобы они не мешали (т.д., т.е., т.п.)
        result = result.replaceAll("т\\.д\\.", "т<DOT>д<DOT>");
        result = result.replaceAll("т\\.е\\.", "т<DOT>е<DOT>");
        result = result.replaceAll("т\\.п\\.", "т<DOT>п<DOT>");

        // 1) Цепочки инициалов: "А. Б." / "А. Б. В." -> "А<DOT>Б<DOT>" / "А<DOT>Б<DOT>В<DOT>"
        result = regexReplaceAll(result, INITIALS_SEQUENCE, mr -> mr.group().replaceAll("\\.\\s*", "<DOT>"));

        // 2) Нумерованные пункты: "1." "35." -> "1<DOT>" "35<DOT>" (не затрагиваем "15.5" — отрицательный lookahead)
        result = regexReplaceAll(result, NUMBER_DOT_PATTERN, mr -> mr.group(1) + "<DOT>");

        // 3) Словарные сокращения (из файла) — простой replace
        for (String abbr : abbrs) {
            if (abbr == null || abbr.isEmpty()) continue;
            String mask = abbr.replace(".", "<DOT>");
            // replace (не regex), чтобы не усложнять экранирование
            result = result.replace(abbr, mask);
        }

        // 4) Одиночные буквенные аббревиатуры (не инициалы)
        result = regexReplaceAll(result, SINGLE_LETTER_ABBR, mr -> mr.group(1) + "<DOT>");

        // 5) Двух/трёхбуквенные аббревиатуры
        result = regexReplaceAll(result, TWO_OR_THREE_LETTER_ABBR, mr -> mr.group(1) + "<DOT>");

        return result;
    }

    /**
     * Восстановление точек обратно (и аккуратное восстановление пробелов между инициалами).
     */
    public String unmask(String text) {
        if (text == null) return null;
        String res = text.replace("<DOT>", ".");

        // Вставляем пробел между инициалами при необходимости: "В.К." -> "В. К."
        // Делаем итеративно, пока находятся пары без пробела.
        String prev;
        do {
            prev = res;
            res = res.replaceAll("([А-ЯЁ])\\.\\s*([А-ЯЁ])\\.", "$1. $2.");
        } while (!res.equals(prev));

        return res;
    }
}