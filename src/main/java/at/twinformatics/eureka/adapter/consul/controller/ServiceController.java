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
package at.twinformatics.eureka.adapter.consul.controller;

import at.twinformatics.eureka.adapter.consul.mapper.InstanceInfoMapper;
import at.twinformatics.eureka.adapter.consul.model.Service;
import at.twinformatics.eureka.adapter.consul.model.ServiceHealth;
import at.twinformatics.eureka.adapter.consul.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import rx.Single;

import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ServiceController {

    private static final String CONSUL_IDX_HEADER = "X-Consul-Index";

    private static final String QUERY_PARAM_WAIT = "wait";
    private static final String QUERY_PARAM_INDEX = "index";

    private static final Pattern WAIT_PATTERN = Pattern.compile("(\\d*)(m|s|ms|h)");
    private static final Random RANDOM = new Random();

    private final RegistrationService registrationService;
    private final InstanceInfoMapper instanceInfoMapper;

    @GetMapping(value = "/v1/catalog/services", produces = MediaType.APPLICATION_JSON_VALUE)
    public Single<ResponseEntity<Map<String, String[]>>> getServiceNames(@QueryParam(QUERY_PARAM_WAIT) String wait,
                                                                         @QueryParam(QUERY_PARAM_INDEX) Long index) {
        return registrationService.getServiceNames(getWaitMillis(wait), index)
                .map(item -> createResponseEntity(item.getItem(), item.getChangeIndex()));
    }

    @GetMapping(value = "/v1/catalog/service/{appName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Single<ResponseEntity<List<Service>>> getService(@PathVariable("appName") String appName,
                                                            @QueryParam(QUERY_PARAM_WAIT) String wait,
                                                            @QueryParam(QUERY_PARAM_INDEX) Long index) {
        Assert.isTrue(appName != null, "service name can not be null");
        return registrationService.getService(appName, getWaitMillis(wait), index)
                .map(item -> {
                    List<Service> services = item.getItem().stream().map(instanceInfoMapper::map).collect(toList());
                    return createResponseEntity(services, item.getChangeIndex());
                });
    }

    @GetMapping(value = "/v1/health/service/{appName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Single<ResponseEntity<List<ServiceHealth>>> getServiceHealth(@PathVariable("appName") String appName,
                                                                        @QueryParam(QUERY_PARAM_WAIT) String wait,
                                                                        @QueryParam(QUERY_PARAM_INDEX) Long index) {
        Assert.isTrue(appName != null, "service name can not be null");
        return registrationService.getService(appName, getWaitMillis(wait), index)
                .map(item -> {
                    List<ServiceHealth> services = item.getItem().stream()
                            .map(instanceInfoMapper::mapToHealth).collect(toList());
                    return createResponseEntity(services, item.getChangeIndex());
                });
    }

    private MultiValueMap<String, String> createHeaders(long index) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(CONSUL_IDX_HEADER, "" + index);
        return headers;
    }

    private <T> ResponseEntity<T> createResponseEntity(T body, long index) {
        return new ResponseEntity<>(body, createHeaders(index), HttpStatus.OK);
    }

    /**
     * Details to the wait behaviour can be found
     * https://www.consul.io/api/index.html#blocking-queries
     */
    private long getWaitMillis(String wait) {
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

    private TimeUnit parseTimeUnit(String unit) {
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
