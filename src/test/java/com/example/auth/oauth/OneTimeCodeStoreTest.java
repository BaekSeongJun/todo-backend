package com.example.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.common.exception.InvalidOAuthCodeException;
import org.junit.jupiter.api.Test;

class OneTimeCodeStoreTest {

    private final OneTimeCodeStore store = new OneTimeCodeStore(90_000);

    @Test
    void issue_후_즉시_consume하면_JWT를_반환한다() {
        String code = store.issue("sample-jwt");

        assertThat(store.consume(code)).isEqualTo("sample-jwt");
    }

    @Test
    void 같은_코드로_두_번째_consume하면_예외가_발생한다() {
        String code = store.issue("sample-jwt");
        store.consume(code);

        assertThatThrownBy(() -> store.consume(code))
                .isInstanceOf(InvalidOAuthCodeException.class);
    }

    @Test
    void TTL_경과_후_consume하면_예외가_발생한다() throws InterruptedException {
        OneTimeCodeStore shortLivedStore = new OneTimeCodeStore(50);
        String code = shortLivedStore.issue("sample-jwt");

        Thread.sleep(100);

        assertThatThrownBy(() -> shortLivedStore.consume(code))
                .isInstanceOf(InvalidOAuthCodeException.class);
    }

    @Test
    void 존재하지_않는_코드는_예외가_발생한다() {
        assertThatThrownBy(() -> store.consume("no-such-code"))
                .isInstanceOf(InvalidOAuthCodeException.class);
    }
}
