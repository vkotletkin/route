package ru.kotletkin.router.xml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XPathMatcherTest {

    private final XPathMatcher matcher = new XPathMatcher();

    @Test
    void matchesOrderAndInvoiceElements() {
        String order = "<document><order id=\"1\"><status>new</status></order></document>";
        String invoice = "<document><invoice id=\"9\"/></document>";

        assertTrue(matcher.matches(order, "//order"));
        assertFalse(matcher.matches(order, "//invoice"));
        assertTrue(matcher.matches(invoice, "//invoice"));
        assertFalse(matcher.matches(invoice, "//order"));
    }

    @Test
    void rejectsMalformedXml() {
        assertThrows(IllegalArgumentException.class, () -> matcher.matches("<order>", "//order"));
    }
}
