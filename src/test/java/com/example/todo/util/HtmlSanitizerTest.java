package com.example.todo.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void sanitize_null이면_null을_반환한다() {
        assertThat(sanitizer.sanitize(null)).isNull();
    }

    @Test
    void sanitize_script_태그는_제거된다() {
        String result = sanitizer.sanitize("<script>alert(1)</script>");
        assertThat(result).doesNotContain("script").doesNotContain("alert");
    }

    @Test
    void sanitize_img_태그는_완전히_제거된다() {
        String result = sanitizer.sanitize("<p>본문<img src=x onerror=alert(1)></p>");
        assertThat(result).doesNotContain("img").doesNotContain("onerror").contains("본문");
    }

    @Test
    void sanitize_javascript_스킴_링크는_href가_제거된다() {
        String result = sanitizer.sanitize("<a href=\"javascript:alert(1)\">클릭</a>");
        assertThat(result).doesNotContain("javascript:").contains("클릭");
    }

    @Test
    void sanitize_onclick같은_on속성은_제거된다() {
        String result = sanitizer.sanitize("<p onclick=\"alert(1)\">내용</p>");
        assertThat(result).doesNotContain("onclick").contains("내용");
    }

    @Test
    void sanitize_허용된_서식_태그는_보존된다() {
        String result =
                sanitizer.sanitize(
                        "<p><b>굵게</b> <i>기울임</i> <u>밑줄</u> <s>취소선</s></p>"
                                + "<ul><li>목록</li></ul><h1>제목</h1><blockquote>인용</blockquote>");

        assertThat(result)
                .contains("<b>굵게</b>")
                .contains("<i>기울임</i>")
                .contains("<u>밑줄</u>")
                .contains("<s>취소선</s>")
                .contains("<li>목록</li>")
                .contains("<h1>제목</h1>")
                .contains("인용");
    }

    @Test
    void sanitize_https_링크는_href속성이_보존된다() {
        String result = sanitizer.sanitize("<a href=\"https://example.com\">링크</a>");

        assertThat(result).contains("href=\"https://example.com\"").contains("링크");
    }
}
