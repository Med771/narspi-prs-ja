package ru.ai.narspiprsja.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@RequiredArgsConstructor
public class RabbitConfig {
    private final String urlsReqQueue = "gtw.query.request.queue";
    private final String pageReqQueue = "gtw.history.request.queue";

    private final String gtwExc = "gtw.exchange";

    private final String urlsReqRoutingKey = "prs.urls.request.routing.key";
    private final String pageReqRoutingKey = "prs.page.request.routing.key";

    @Bean
    public Queue urlsRequestQueue() {
        return QueueBuilder.durable(urlsReqQueue).build();
    }

    @Bean
    public Queue pageRequestQueue() {
        return QueueBuilder.durable(pageReqQueue).build();
    }

    @Bean
    public DirectExchange gtwExchange() {
        return new DirectExchange(gtwExc);
    }

    @Bean
    public Binding urlsRequestBinding(Queue urlsRequestQueue, DirectExchange gtwExchange) {
        return BindingBuilder.bind(urlsRequestQueue)
                .to(gtwExchange)
                .with(urlsReqRoutingKey);
    }

    @Bean
    public Binding pageRequestBinding(Queue pageRequestQueue, DirectExchange gtwExchange) {
        return BindingBuilder.bind(pageRequestQueue)
                .to(gtwExchange)
                .with(pageReqRoutingKey);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setReplyTimeout(5000);
        return template;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
