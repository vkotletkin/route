package ru.kotletkin.router.xml;

import net.sf.saxon.s9api.*;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class XPathMatcher {

    private final Processor processor = new Processor(false);
    private final ConcurrentHashMap<String, XPathExecutable> compiled = new ConcurrentHashMap<>();

    public boolean matches(String xml, String expression) {
        XdmNode document = parse(xml);
        try {
            XPathSelector selector = compiled.computeIfAbsent(expression, this::compile).load();
            selector.setContextItem(document);
            XdmValue result = selector.evaluate();
            return !result.isEmptySequence();
        } catch (SaxonApiException exception) {
            throw new IllegalStateException("Failed to evaluate XPath: " + expression, exception);
        }
    }

    private XdmNode parse(String xml) {
        try {
            return processor.newDocumentBuilder().build(new StreamSource(new StringReader(xml)));
        } catch (SaxonApiException exception) {
            throw new IllegalArgumentException("XML payload is not well-formed", exception);
        }
    }

    private XPathExecutable compile(String expression) {
        try {
            return processor.newXPathCompiler().compile(expression);
        } catch (SaxonApiException exception) {
            throw new IllegalStateException("XPath is invalid: " + expression, exception);
        }
    }
}
