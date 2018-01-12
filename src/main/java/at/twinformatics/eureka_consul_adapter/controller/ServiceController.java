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
package at.twinformatics.eureka_consul_adapter.controller;

import com.netflix.discovery.shared.Application;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import lombok.RequiredArgsConstructor;
import at.twinformatics.eureka_consul_adapter.event.ServiceChangeDetector;
import at.twinformatics.eureka_consul_adapter.mapper.ServiceMapper;
import at.twinformatics.eureka_consul_adapter.model.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.QueryParam;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ServiceController {

    private static final String CONSUL_IDX_HEADER = "X-Consul-Index";

    private static final String QUERY_PARAM_WAIT = "wait";
    private static final String QUERY_PARAM_INDEX = "index";

    private static final Pattern WAIT_PATTERN = Pattern.compile("(\\d*)(m|s|ms|h)");
    private static final Random RANDOM = new Random();
    private static final String[] NO_SERVICE_TAGS = new String[0];

    private final PeerAwareInstanceRegistry registry;
    private final ServiceChangeDetector serviceChangeDetector;
    private final ServiceMapper serviceMapper;

    @GetMapping(value = "/v1/catalog/services", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String[]> getServiceNames(@QueryParam(QUERY_PARAM_WAIT) String wait,
                                                 @QueryParam(QUERY_PARAM_INDEX) Long index,
                                                 HttpServletResponse response) {

        return blockUntilChangeOrTimeout(wait, index, response, serviceChangeDetector::getLastEmitted,
                serviceChangeDetector::getOrWait,
                () -> registry.getApplications().getRegisteredApplications().stream()
                        .collect(toMap(Application::getName, a -> NO_SERVICE_TAGS)));
    }

    @GetMapping(value = "/v1/catalog/service/{appName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Service> getServices(@PathVariable("appName") String appName,
                                     @QueryParam(QUERY_PARAM_WAIT) String wait,
                                     @QueryParam(QUERY_PARAM_INDEX) Long index,
                                     HttpServletResponse response) {

        return blockUntilChangeOrTimeout(wait, index, response, () -> serviceChangeDetector.getLastEmittedOfApp(appName),
                waitMillis -> serviceChangeDetector.getOrWait(appName, waitMillis),
                () -> {
                    Application application = registry.getApplication(appName);
                    if (application == null) {
                        return Collections.emptyList();
                    } else {
                        return application.getInstances().stream().map(serviceMapper::map).collect(toList());
                    }
                });
    }

    <T> T blockUntilChangeOrTimeout(String wait, Long index, HttpServletResponse response,
                                    Supplier<Long> lastEmitted, Function<Long, Long> waitOrGet,
                                    Supplier<T> fn) {
        if (index == null || lastEmitted.get() > index) {
            response.setHeader(CONSUL_IDX_HEADER, String.valueOf(lastEmitted.get()));
            return fn.get();
        } else {
            Long newIndex = waitOrGet.apply(getWaitMillis(wait));
            response.setHeader(CONSUL_IDX_HEADER, String.valueOf(newIndex));
            return fn.get();
        }
    }

    /**
     * Details to the wait behaviour can be found
     * https://www.consul.io/api/index.html#blocking-queries
     */
    long getWaitMillis(String wait) {
        // default from consul docu
        long millis = TimeUnit.MINUTES.toMillis(5);
        if (wait != null) {
            Matcher matcher = WAIT_PATTERN.matcher(wait);
            if (matcher.matches()) {
                Long value = Long.valueOf(matcher.group(1));
                TimeUnit timeUnit = parseTimeUnit(matcher.group(2));
                millis = timeUnit.toMillis(value);
            } else {
                throw new IllegalArgumentException("Invalid wait pattern");
            }
        }
        return millis + RANDOM.nextInt(((int) millis / 16) + 1);
    }

    TimeUnit parseTimeUnit(String unit) {
        switch (unit) {
            case "h":
                return TimeUnit.HOURS;
            case "m":
                return TimeUnit.MINUTES;
            case "s":
                return TimeUnit.SECONDS;
            case "ms":
                return TimeUnit.MILLISECONDS;
            default:
                throw new IllegalArgumentException("No valid time unit");
        }
    }
}
