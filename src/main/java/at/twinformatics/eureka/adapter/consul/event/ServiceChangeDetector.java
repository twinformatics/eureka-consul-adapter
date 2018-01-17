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
package at.twinformatics.eureka.adapter.consul.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rx.subjects.PublishSubject;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
@RequiredArgsConstructor
public class ServiceChangeDetector {

    private final PublishSubject<String> eventStream = PublishSubject.create();

    private final ChangeCounter changeCounter;

    public void publish(String appName) {

        if (log.isDebugEnabled()) {
            log.debug("Incrementing change counter: appname {}", appName);
        }

        changeCounter.registerChange(appName);

        eventStream.onNext(appName);
    }

    public Long getOrWait(String appName, long millis) {
        // waits for change of app A or x seconds
        return eventStream
                .filter(se -> se.equals(appName))
                .timeout(millis, TimeUnit.MILLISECONDS)
                .onErrorReturn(err -> mapErrorToAppChange(err, Optional.of(appName)))
                .map(se -> changeCounter.getChangeCount(se))
                .toBlocking()
                .first();
    }

    public Long getOrWait(long millis) {
        // waits for change or x seconds
        return eventStream
                .timeout(millis, TimeUnit.MILLISECONDS)
                .onErrorReturn(err -> mapErrorToAppChange(err, Optional.empty()))
                .map(se -> changeCounter.getTotalCount())
                .toBlocking()
                .first();
    }

    private String mapErrorToAppChange(Throwable err, Optional<String> appName){
        if (err instanceof TimeoutException) {
            return "";
        }
        throw new RuntimeException(err.getMessage(), err);
    }

}
