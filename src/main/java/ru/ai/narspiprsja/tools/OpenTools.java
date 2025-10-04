package ru.ai.narspiprsja.tools;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import ru.ai.narspiprsja.config.OpenConfig;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenTools {
    private final OpenConfig openConfig;

    private SentenceDetectorME sentenceDetector;

    @PostConstruct
    public void init() throws Exception {
        try (InputStream sentIn = new ClassPathResource("models/opennlp-ru-ud-gsd-sentence-1.3-2.5.4.bin").getInputStream()) {
            sentenceDetector = new SentenceDetectorME(new SentenceModel(sentIn));
        }
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        String t = Normalizer.normalize(text, Normalizer.Form.NFC);
        t = t.replaceAll("[\\p{C}&&[^\r\n\t]]+", " ");
        t = t.replace('\u2013', '-').replace('\u2014', '-');
        t = t.replace('«', '"').replace('»', '"').replace('“', '"').replace('”', '"');
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }

    /**
     * Детектируем предложения, затем делаем пост-обработку, чтобы склеить
     * ложные разрезы (например после запятой, после нумерации и т.п.).
     */
    public List<String> detectSentences(String text) {
        String cleaned = normalizeText(text);
        String masked = openConfig.mask(cleaned);

        Span[] spans = sentenceDetector.sentPosDetect(masked);
        List<String> sents = new ArrayList<>();
        for (Span s : spans) {
            String part = masked.substring(s.getStart(), s.getEnd()).trim();
            sents.add(openConfig.unmask(part));
        }

        // Пост-обработка: сливаем очевидно неверные разрывы
        List<String> merged = new ArrayList<>();
        for (String cur : sents) {
            if (merged.isEmpty()) {
                merged.add(cur);
                continue;
            }

            String prev = merged.getLast();

            if (shouldMerge(prev, cur)) {
                // склеиваем через пробел
                merged.set(merged.size() - 1, (prev + " " + cur).trim());
            } else {
                merged.add(cur);
            }
        }

        return merged;
    }

    /**
     * Пост-обработка: объединение слишком коротких чанков с предыдущим
     *
     * @param chunks список чанков
     * @param minSize минимальный размер чанка (например 80)
     * @return новый список чанков
     */
    public List<String> mergeShortChunks(List<String> chunks, int minSize) {
        List<String> merged = new ArrayList<>();
        for (String chunk : chunks) {
            if (chunk == null || chunk.trim().isEmpty()) continue;

            if (chunk.length() < minSize && !merged.isEmpty()) {
                // приклеиваем к последнему чанк
                int lastIndex = merged.size() - 1;
                String updated = (merged.get(lastIndex) + " " + chunk).trim();
                merged.set(lastIndex, updated);
            } else {
                merged.add(chunk.trim());
            }
        }
        return merged;
    }


    private boolean shouldMerge(String prev, String cur) {
        if (prev == null || cur == null) return false;

        String prevTrim = prev.trim();
        String curTrim = cur.trim();
        if (prevTrim.isEmpty() || curTrim.isEmpty()) return false;

        char last = lastNonSpaceChar(prevTrim);
        if (last == ',' || last == ';' || last == ':' || last == '—' || last == '-') {
            return true;
        }

        Character firstLetter = firstAlphabeticChar(curTrim);
        if (firstLetter != null && Character.isLowerCase(firstLetter)) {
            return true;
        }

        return curTrim.length() <= 3;
    }

    private char lastNonSpaceChar(String s) {
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c)) return c;
        }
        return '\0';
    }

    private Character firstAlphabeticChar(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) return c;
        }
        return null;
    }
}
