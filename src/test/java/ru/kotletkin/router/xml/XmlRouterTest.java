package ru.kotletkin.router.xml;

import org.junit.jupiter.api.Test;
import ru.kotletkin.router.config.RouteProperties;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XmlRouterTest {

    private static final String BOTH = "<document><order id=\"1\"/><invoice id=\"9\"/></document>";

    private static List<String> queues(List<Route> routes) {
        return routes.stream().map(Route::queue).toList();
    }

    private static XmlRouter router(String json) {
        RouteProperties properties = new RouteProperties(json);
        return new XmlRouter(new XPathMatcher(), properties, JsonMapper.shared());
    }

    @Test
    void keepsEveryMatchWhenPrioritiesAreEqual() {
        XmlRouter router = router("""
                {"orders":"//order","invoices":"//invoice"}
                """);

        assertEquals(List.of("orders", "invoices"), queues(router.destinations(BOTH)));
    }

    @Test
    void selectsTheHighestPriority() {
        XmlRouter router = router("""
                {"orders":{"xpath":"//order","priority":1},"invoices":{"xpath":"//invoice","priority":2}}
                """);

        List<Route> matched = router.destinations(BOTH);

        assertEquals(List.of("invoices"), queues(matched));
        assertEquals(2, matched.getFirst().priority());
    }

    @Test
    void rejectsANonIntegerPriority() {
        assertThrows(IllegalStateException.class, () -> router("""
                {"orders":{"xpath":"//order","priority":"high"}}
                """));
    }
}
