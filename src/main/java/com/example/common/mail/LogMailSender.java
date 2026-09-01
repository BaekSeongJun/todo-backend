package com.example.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발용 구현체. app.mail-type=log(기본값)일 때 활성화되며
 * 실제 메일을 발송하는 대신 링크가 포함된 본문을 로그로 출력한다(FR-R01).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail-type", havingValue = "log", matchIfMissing = true)
public class LogMailSender implements MailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("[MAIL] to={} subject={}\n{}", to, subject, body);
    }
}
