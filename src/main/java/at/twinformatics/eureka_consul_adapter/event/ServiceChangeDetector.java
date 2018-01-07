package at.twinformatics.eureka_consul_adapter.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
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
                app -> {
                    changeCounters.computeIfAbsent(app.getName(), x -> new AtomicLong(INITIAL_VALUE))
                            .set(app.getTimestamp());
                    if (log.isDebugEnabled()) {
                        log.debug("Incrementing change counter: appname {}, value {}",
                                app.getName(), changeCounters.get(app.getName()));
                    }
                });
    }

    public void publish(String appName, long timestamp) {

        if (log.isDebugEnabled()) {
            log.debug("New change detected: appname {}, timestamp {}", appName, timestamp);
        }
        eventStream.onNext(new ServiceChange(appName, timestamp));
    }

    public Long getOrWait(String appName, long millis) {
        // waits for change of app A or x seconds
        eventStream
                .timeout(millis, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(Observable.from(new ServiceChange[]{new ServiceChange(appName, -1)}))
                .toBlocking().first(s -> s.getName().equals(appName)).getName();
        return getLastEmittedOfApp(appName);
    }

    public Long getOrWait(long millis) {
        // waits for change or x seconds
        eventStream.timeout(millis, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(Observable.from(new ServiceChange[]{new ServiceChange("", -1)}))
                .toBlocking().first();
        return getLastEmitted();
    }

    public Long getLastEmitted() {
        // get highest change counter of all apps
        Long lastEmitted = changeCounters.values().stream().mapToLong(AtomicLong::get).max().orElse(INITIAL_VALUE);
        if (log.isDebugEnabled()) {
            log.debug("Last emitted change counter of services {}", lastEmitted);
        }
        return lastEmitted;
    }

    public Long getLastEmittedOfApp(String appName) {
        // get change counter of app A
        long lastEmittedOfApp = changeCounters.getOrDefault(appName, new AtomicLong(INITIAL_VALUE)).longValue();
        if (log.isDebugEnabled()) {
            log.debug("Last emitted change counter of service {}: {}", appName, lastEmittedOfApp);
        }
        return lastEmittedOfApp;
    }

    public void reset() {
        changeCounters.clear();
    }
}
