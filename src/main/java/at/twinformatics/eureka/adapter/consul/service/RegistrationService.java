/**
 * The MIT License
 * Copyright © 2018 Twinformatics GmbH
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package at.twinformatics.eureka.adapter.consul.service;

import at.twinformatics.eureka.adapter.consul.model.ChangeItem;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import rx.Observable;
import rx.Single;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toMap;

/**
 * Returns Services and List of Service with its last changed
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private static final BinaryOperator<String[]> MERGE_FUNCTION = (u,v) -> {
        throw new IllegalStateException("Duplicate key");
    };

    private static final String[] NO_SERVICE_TAGS = new String[0];

    private final PeerAwareInstanceRegistry registry;
    private final ServiceChangeDetector serviceChangeDetector;

    public Single<ChangeItem<Map<String, String[]>>> getServiceNames(long waitMillis, Long index) {
        return returnDeferred(waitMillis, index, serviceChangeDetector::getLastEmitted,
                serviceChangeDetector::getTotalIndex,
                () -> registry.getApplications().getRegisteredApplications().stream()
                        .collect(toMap(Application::getName, a -> NO_SERVICE_TAGS, MERGE_FUNCTION, TreeMap::new)));
    }

    public Single<ChangeItem<List<InstanceInfo>>> getService(String appName, long waitMillis, Long index) {

        return returnDeferred(waitMillis, index, () -> serviceChangeDetector.getLastEmittedOfApp(appName),
                waitMillisInternal -> serviceChangeDetector.getIndexOfApp(appName, waitMillisInternal),
                () -> {
                    Application application = registry.getApplication(appName);
                    if (application == null) {
                        return Collections.emptyList();
                    } else {
                        return new ArrayList<>(application.getInstances());
                    }
                });
    }

    private <T> Single<ChangeItem<T>> returnDeferred(long waitMillis, Long index,
                                                     Supplier<Long> lastEmitted, Function<Long, Observable<Long>> supplyObservable,
                                                     Supplier<T> fn) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        if (index == null || !lastEmitted.get().equals(index)) {
            return Single.just(new ChangeItem<>(fn.get(), lastEmitted.get()));
        } else {
            return supplyObservable.apply(waitMillis)
                    .map(idx -> new ChangeItem<>(fn.get(), idx))
                    .first()
                    .toSingle();
        }
    }
}
