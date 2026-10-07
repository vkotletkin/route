package ru.kotletkin.router.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;
import ru.kotletkin.router.xml.Route;
import ru.kotletkin.router.xml.XmlRouter;

import java.util.List;
import java.util.function.Consumer;

@Configuration
public class XmlInboundConfiguration {

    static final String OUTPUT_BINDING = "xmlOutbound-out-0";
    static final String ROUTE_KEY = "routeKey";

    private static final Logger log = LoggerFactory.getLogger(XmlInboundConfiguration.class);

    @Bean
    Consumer<String> xmlInbound(XmlRouter router, StreamBridge bridge) {
        return xml -> route(xml, router, bridge);
    }

    private static void route(String xml, XmlRouter router, StreamBridge bridge) {
        List<Route> destinations;
        try {
            destinations = router.destinations(xml);
        } catch (IllegalArgumentException exception) {
            log.error("Skip malformed XML", exception);
            return;
        }
        if (destinations.isEmpty()) {
            log.info("No XPath route matched the XML payload");
            return;
        }
        for (Route destination : destinations) {
            log.info("Publishing XML to routing key {} (priority {})", destination.queue(), destination.priority());
            bridge.send(OUTPUT_BINDING, MessageBuilder.withPayload(xml)
                    .setHeader(ROUTE_KEY, destination.queue())
                    .build());
        }
    }
}
