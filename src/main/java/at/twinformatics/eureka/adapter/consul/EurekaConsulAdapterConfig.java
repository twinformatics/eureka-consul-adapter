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
package at.twinformatics.eureka.adapter.consul;

import at.twinformatics.eureka.adapter.consul.controller.AgentController;
import at.twinformatics.eureka.adapter.consul.controller.ServiceController;
import at.twinformatics.eureka.adapter.consul.event.RegistrationEventInstanceRegistry;
import at.twinformatics.eureka.adapter.consul.event.ServiceChangeDetector;
import at.twinformatics.eureka.adapter.consul.mapper.ServiceMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.ServerCodecs;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.Assert;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
@EnableAsync
public class EurekaConsulAdapterConfig {

    @Bean
    @ConditionalOnMissingBean
    public AgentController agentController() {
        return new AgentController();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceController serviceController(PeerAwareInstanceRegistry peerAwareInstanceRegistry) {
        Assert.isTrue(peerAwareInstanceRegistry instanceof RegistrationEventInstanceRegistry,
                "Instance Registry must be of type" + RegistrationEventInstanceRegistry.class.getName());
        return new ServiceController(peerAwareInstanceRegistry, serviceChangeDetector(), serviceMapper());
    }

    @Bean
    @Primary
    public RegistrationEventInstanceRegistry registrationEventInstanceRegistry(
            ServerCodecs serverCodecs, EurekaServerConfig eurekaServerConfig, EurekaClient eurekaClient,
            EurekaClientConfig eurekaClientConfig, InstanceRegistryProperties instanceRegistryProperties,
            ServiceChangeDetector serviceChangeDetector) {
        eurekaClient.getApplications(); // force initialization
        return new RegistrationEventInstanceRegistry(eurekaServerConfig, eurekaClientConfig,
                serverCodecs, eurekaClient,
                instanceRegistryProperties.getExpectedNumberOfRenewsPerMin(),
                instanceRegistryProperties.getDefaultOpenForTrafficCount(), serviceChangeDetector);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceMapper serviceMapper() {
        return new ServiceMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceChangeDetector serviceChangeDetector() {
        return new ServiceChangeDetector();
    }

}
