package com.example.todo.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class HtmlSanitizer {

    private static final Safelist SAFELIST =
            new Safelist()
                    .addTags(
                            "b", "strong", "i", "em", "u", "s", "strike", "del", "ul", "ol", "li",
                            "h1", "h2", "h3", "blockquote", "a", "p", "br")
                    .addAttributes("a", "href")
                    .addProtocols("a", "href", "http", "https");

    public String sanitize(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, SAFELIST);
    }
}
