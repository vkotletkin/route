package ru.kotletkin.router;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@EnableTestBinder
class RouterApplicationTests {

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Test
    void contextLoads() {
    }

    @Test
    void routesOrderAndInvoiceToSeparateRoutingKeys() {
        send("<document><order id=\"1\"/></document>");
        Message<byte[]> order = output.receive(5_000, "xml.out");
        assertEquals("orders", order.getHeaders().get("routeKey"));
        assertTrue(new String(order.getPayload(), StandardCharsets.UTF_8).contains("order"));

        send("<document><invoice id=\"9\"/></document>");
        Message<byte[]> invoice = output.receive(5_000, "xml.out");
        assertEquals("invoices", invoice.getHeaders().get("routeKey"));
        assertTrue(new String(invoice.getPayload(), StandardCharsets.UTF_8).contains("invoice"));
    }

    @Test
    void publishesOnlyTheHighestPriorityRoute() {
        send("<document><order id=\"1\"/><invoice id=\"9\"/></document>");

        Message<byte[]> winner = output.receive(5_000, "xml.out");
        assertEquals("invoices", winner.getHeaders().get("routeKey"));
        assertNull(output.receive(200, "xml.out"));
    }

    @Test
    void skipsMalformedXml() {
        send("<order>");
        assertNull(output.receive(500, "xml.out"));
    }

    private void send(String xml) {
        input.send(MessageBuilder.withPayload(xml).build());
    }

    private String routingKey() {
        Message<byte[]> message = output.receive(5_000, "xml.out");
        return String.valueOf(message.getHeaders().get("routeKey"));
    }
}
