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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.resources.ServerCodecs;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistry;
import org.springframework.stereotype.Component;

@Component
public class RegistrationEventInstanceRegistry extends InstanceRegistry {

    private final ServiceChangeDetector serviceChangeDetector;

    public RegistrationEventInstanceRegistry(EurekaServerConfig serverConfig, EurekaClientConfig clientConfig,
                                             ServerCodecs serverCodecs, EurekaClient eurekaClient,
                                             int expectedNumberOfRenewsPerMin, int defaultOpenForTrafficCount,
                                             ServiceChangeDetector serviceChangeDetector) {
        super(serverConfig, clientConfig, serverCodecs, eurekaClient, expectedNumberOfRenewsPerMin,
                defaultOpenForTrafficCount);
        this.serviceChangeDetector = serviceChangeDetector;
    }

    @Override
    public void register(InstanceInfo info, boolean isReplication) {
        super.register(info, isReplication);
        serviceChangeDetector.publish(info.getAppName(), System.currentTimeMillis());
    }

    @Override
    public void register(InstanceInfo info, int leaseDuration, boolean isReplication) {
        super.register(info, leaseDuration, isReplication);
        serviceChangeDetector.publish(info.getAppName(), System.currentTimeMillis());
    }

    @Override
    public boolean cancel(String appName, String serverId, boolean isReplication) {
        boolean wasCancelled = super.cancel(appName, serverId, isReplication);
        serviceChangeDetector.publish(appName, System.currentTimeMillis());
        return wasCancelled;
    }
}
