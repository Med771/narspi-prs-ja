package ru.ai.narspiprsja;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.ai.narspiprsja.model.Url;
import ru.ai.narspiprsja.tools.ParserTools;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StartupRunner implements ApplicationRunner {
    private final ParserTools parserTools;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<Url> urls = parserTools.parseNewsPage("culture");

        for (Url url : urls) {
            System.out.println(url.link());
            System.out.println(url.date());
        }
    }
}
