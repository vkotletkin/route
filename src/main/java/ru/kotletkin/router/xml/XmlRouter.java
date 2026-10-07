package ru.kotletkin.router.xml;

import org.springframework.stereotype.Component;
import ru.kotletkin.router.config.RouteProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

@Component
public class XmlRouter {

    private final XPathMatcher matcher;
    private final List<Route> routes;

    public XmlRouter(XPathMatcher matcher, RouteProperties properties, JsonMapper jsonMapper) {
        this.matcher = matcher;
        this.routes = readRoutes(properties.getRoutesJson(), jsonMapper);
    }

    public List<Route> destinations(String xml) {
        List<Route> matched = new ArrayList<>();
        for (Route route : routes) {
            if (matcher.matches(xml, route.xpath())) {
                matched.add(route);
            }
        }
        if (matched.isEmpty()) {
            return List.of();
        }
        int highest = matched.stream().mapToInt(Route::priority).max().orElseThrow();
        return matched.stream()
                .filter(route -> route.priority() == highest)
                .toList();
    }

    private static List<Route> readRoutes(String json, JsonMapper jsonMapper) {
        if (json == null || json.isBlank()) {
            throw new IllegalStateException("APP_ROUTES_JSON must be a JSON object of queue name to XPath");
        }
        JsonNode root;
        try {
            root = jsonMapper.readTree(json);
        } catch (JacksonException exception) {
            throw new IllegalStateException("APP_ROUTES_JSON must be a JSON object of queue name to XPath", exception);
        }
        if (!root.isObject() || root.isEmpty()) {
            throw new IllegalStateException("APP_ROUTES_JSON must contain at least one queue");
        }
        List<Route> parsed = new ArrayList<>();
        for (var field : root.properties()) {
            parsed.add(readRoute(field.getKey(), field.getValue()));
        }
        return List.copyOf(parsed);
    }

    private static Route readRoute(String queue, JsonNode value) {
        if (queue == null || queue.isBlank()) {
            throw new IllegalStateException("APP_ROUTES_JSON entries must have a queue name and an XPath");
        }
        if (value.isString()) {
            String xpath = value.asString();
            if (xpath.isBlank()) {
                throw new IllegalStateException("APP_ROUTES_JSON entries must have a queue name and an XPath");
            }
            return new Route(queue, xpath, 0);
        }
        if (!value.isObject()) {
            throw new IllegalStateException("Route " + queue + " must be an XPath string or an object with xpath and priority");
        }
        JsonNode xpathNode = value.get("xpath");
        if (xpathNode == null || !xpathNode.isString() || xpathNode.asString().isBlank()) {
            throw new IllegalStateException("Route " + queue + " must include an xpath");
        }
        return new Route(queue, xpathNode.asString(), readPriority(queue, value.get("priority")));
    }

    private static int readPriority(String queue, JsonNode priority) {
        if (priority == null || priority.isNull()) {
            return 0;
        }
        if (!priority.isIntegralNumber()) {
            throw new IllegalStateException("Route " + queue + " priority must be an integer");
        }
        return priority.asInt();
    }
}
