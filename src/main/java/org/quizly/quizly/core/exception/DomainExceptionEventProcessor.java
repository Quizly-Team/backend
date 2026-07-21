package org.quizly.quizly.core.exception;

import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DomainExceptionEventProcessor implements EventProcessor {

    @Override
    public SentryEvent process(SentryEvent event, Hint hint) {
        if (event.getThrowable() instanceof DomainException e) {
            event.setFingerprints(List.of(e.getCode()));
            event.setTag("errorCode", e.getCode());
        }
        return event;
    }
}
