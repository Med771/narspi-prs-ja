package ru.ai.narspiprsja.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "parse.config")
public class ParserProperty {
    private String date;
    private List<String> parts;

    private String splitter;
    private int maxTokens;
    private int overlapTokens;
}
