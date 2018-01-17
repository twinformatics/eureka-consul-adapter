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

import at.twinformatics.eureka.adapter.consul.event.CanceledEventHandler;
import at.twinformatics.eureka.adapter.consul.event.ChangeCounter;
import at.twinformatics.eureka.adapter.consul.event.RegisteredEventHandler;
import at.twinformatics.eureka.adapter.consul.event.ServiceChangeDetector;
import at.twinformatics.eureka.adapter.consul.controller.AgentController;
import at.twinformatics.eureka.adapter.consul.controller.ServiceController;
import at.twinformatics.eureka.adapter.consul.mapper.ServiceMapper;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EurekaConsulAdapterConfig {

    @Bean
    @ConditionalOnMissingBean
    public AgentController agentController() {
        return new AgentController();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceController serviceController(PeerAwareInstanceRegistry peerAwareInstanceRegistry) {
        return new ServiceController(peerAwareInstanceRegistry, serviceChangeDetector(),
                serviceMapper(), changeCounter());
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceMapper serviceMapper() {
        return new ServiceMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceChangeDetector serviceChangeDetector() {
        return new ServiceChangeDetector(changeCounter());
    }

    @Bean
    @ConditionalOnMissingBean
    public RegisteredEventHandler registeredEventHandler() {
        return new RegisteredEventHandler(serviceChangeDetector());
    }

    @Bean
    @ConditionalOnMissingBean
    public CanceledEventHandler canceledEventHandler() {
        return new CanceledEventHandler(serviceChangeDetector());
    }

    @Bean
    @ConditionalOnMissingBean
    public ChangeCounter changeCounter() {
        return new ChangeCounter();
    }
}
