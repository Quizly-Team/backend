package org.quizly.quizly.core.util;

import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@Log4j2
public class SsePublisher {

    public void sendChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event()
                .name("message")
                .data(chunk)
            );
        } catch (IOException e) {
            log.error("chunk 전송 중 에러 발생", e);
        }
    }

}
