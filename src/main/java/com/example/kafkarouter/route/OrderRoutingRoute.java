package com.example.kafkarouter.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class OrderRoutingRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("kafka:orders?brokers=localhost:9092&groupId=order-processor")
                .choice()
                .when(simple("${body} contains 'us'"))
                .to("kafka:orders.us?brokers=localhost:9092")
                .when(simple("${body} contains 'eu'"))
                .to("kafka:orders.eu?brokers=localhost:9092")
                .otherwise()
                .to("kafka:orders.other?brokers=localhost:9092");

    }
}