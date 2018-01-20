/**
 * The MIT License
 * Copyright © 2018 Twinformatics GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package at.twinformatics.eureka.adapter.consul.service;

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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class ServiceChangeDetector {

    private static final long INITIAL_VALUE = 1L;

    private final PublishSubject<ServiceChange> eventStream = PublishSubject.create();
    private final Map<String, AtomicLong> changeCounters = Collections.synchronizedMap(new HashMap<>());

    public void publish(String appName, long timestamp) {

        ServiceChange change = new ServiceChange(appName, timestamp);

        if (log.isDebugEnabled()) {
            log.debug("Incrementing change counter: appname {}, value {}",
                    change.getName(), changeCounters.get(change.getName()));
        }

        changeCounters.computeIfAbsent(change.getName(), x -> new AtomicLong(INITIAL_VALUE))
                .set(change.getTimestamp());

        eventStream.onNext(change);
    }

    public Observable<Long> getIndexOfApp(String appName, long millis) {
        // waits for change of app A or x seconds
        return eventStream
                .filter(se -> se.getName().equals(appName))
                .timeout(millis, TimeUnit.MILLISECONDS)
                .onErrorReturn(err -> mapTimeoutToServiceChange(err, appName))
                .map(se -> getLastEmittedOfApp(se.getName()));
    }

    public Observable<Long> getTotalIndex(long millis) {
        // waits for change or x seconds
        return eventStream
                .timeout(millis, TimeUnit.MILLISECONDS)
                .onErrorReturn(err -> mapTimeoutToServiceChange(err, ""))
                .map(se -> getLastEmitted());
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

    private ServiceChange mapTimeoutToServiceChange(Throwable err, String appName){
        if (err instanceof TimeoutException) {
            return new ServiceChange(appName, -1);
        }
        throw new RuntimeException(err.getMessage(), err);
    }

    @AllArgsConstructor
    @Getter
    private static class ServiceChange {
        private String name;
        private long timestamp;
    }
}
