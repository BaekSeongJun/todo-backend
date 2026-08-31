package com.example.auth.oauth;

import com.example.common.exception.InvalidOAuthCodeException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class OneTimeCodeStore {

    private static final long DEFAULT_TTL_MS = 90_000; // 90초, PRD "1~2분" 범위 내

    private record CodeEntry(String jwt, long expiresAtEpochMs) {}

    private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();
    private final long ttlMs;

    public OneTimeCodeStore() {
        this(DEFAULT_TTL_MS);
    }

    OneTimeCodeStore(long ttlMs) { // package-private, 테스트에서 짧은 TTL 주입용
        this.ttlMs = ttlMs;
    }

    public String issue(String jwt) {
        String code = UUID.randomUUID().toString();
        store.put(code, new CodeEntry(jwt, System.currentTimeMillis() + ttlMs));
        return code;
    }

    public String consume(String code) {
        // remove 자체가 원자적 연산이라 동시 요청 중 하나만 entry를 가져가므로 별도 synchronized 불필요
        CodeEntry entry = store.remove(code);
        if (entry == null || entry.expiresAtEpochMs() < System.currentTimeMillis()) {
            throw new InvalidOAuthCodeException();
        }
        return entry.jwt();
    }
}
