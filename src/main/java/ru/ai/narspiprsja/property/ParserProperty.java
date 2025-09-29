package ru.ai.narspiprsja.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "parse.config")
public class ParserProperty {
    private String date;
}
