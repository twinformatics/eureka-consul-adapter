package net.tine2k.eureka_consul_adapter.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ServiceChangeDetector {

    private static final long INITIAL_VALUE = 1L;

    private final PublishSubject<ServiceChange> eventStream = PublishSubject.create();
    private final Map<String, AtomicLong> changeCounters = Collections.synchronizedMap(new HashMap<>());

    @AllArgsConstructor
    @Getter
    private static class ServiceChange {
        private String name;
        private long timestamp;
    }

    public ServiceChangeDetector() {
        eventStream.subscribe(
                app -> changeCounters.computeIfAbsent(app.getName(), x -> new AtomicLong(INITIAL_VALUE))
                        .set(app.getTimestamp()));
    }

    public void publish(String appName, long timestamp) {
        eventStream.onNext(new ServiceChange(appName, timestamp));
    }

    public Long getOrWait(String appName, long millis) {
        return getLastEmitted(eventStream
                .timeout(millis, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(Observable.from(new ServiceChange[]{new ServiceChange(appName, INITIAL_VALUE)}))
                .toBlocking().first(s -> s.getName().equals(appName)).getName());
    }

    public Long getOrWait(long millis) {
        eventStream.timeout(millis, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(Observable.from(new ServiceChange[]{new ServiceChange("", INITIAL_VALUE)}))
                .toBlocking().first();
        return getLastEmitted();
    }

    public Long getLastEmitted() {
        return changeCounters.values().stream().mapToLong(AtomicLong::get).max().orElse(INITIAL_VALUE);
    }

    public Long getLastEmitted(String appName) {
        return changeCounters.getOrDefault(appName, new AtomicLong(INITIAL_VALUE)).longValue();
    }

    public void reset() {
        changeCounters.clear();
    }
}
