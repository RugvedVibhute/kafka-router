package com.example.kafkarouter.config;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@Configuration
@EnableKafkaStreams
public class StreamsConfig {

    private static final Logger logger = LoggerFactory.getLogger(StreamsConfig.class);

    @Bean
    public KStream<String, String> orderRoutingStream(StreamsBuilder builder) {
        logger.info("Setting up order routing streams...");

        KStream<String, String> orders = builder.stream("orders");

        // Log all incoming orders
        orders.peek((key, value) -> logger.info("Processing order: {}", value));

        // Route US orders
        orders.filter((key, value) -> value.contains("us"))
                .peek((key, value) -> logger.info("Routing to US: {}", value))
                .to("orders.us");

        // Route EU orders
        orders.filter((key, value) -> value.contains("eu"))
                .peek((key, value) -> logger.info("Routing to EU: {}", value))
                .to("orders.eu");

        // Route everything else
        orders.filter((key, value) -> !value.contains("us") && !value.contains("eu"))
                .peek((key, value) -> logger.info("Routing to OTHER: {}", value))
                .to("orders.other");

        logger.info("Order routing streams configured successfully");
        return orders;
    }
}