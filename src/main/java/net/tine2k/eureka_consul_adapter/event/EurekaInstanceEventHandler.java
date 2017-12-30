package net.tine2k.eureka_consul_adapter.event;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class EurekaInstanceEventHandler {

    private final ServiceChangeDetector serviceChangeDetector;

    @Bean
    public ApplicationListener<EurekaInstanceRegisteredEvent> onRegisteredEvent() {
        return event -> serviceChangeDetector.publish(event.getInstanceInfo().getAppName(), event.getTimestamp());
    }

    @Bean
    public ApplicationListener<EurekaInstanceCanceledEvent> onCanceledEvent() {
        return event -> serviceChangeDetector.publish(event.getAppName(), event.getTimestamp());
    }

    // Ignore EurekaInstanceRenewedEvent - only renews the lease
//    @Bean
//    public ApplicationListener<EurekaInstanceRenewedEvent> onRenewedEvent() {
//        return event -> serviceChangeDetector.publish(atomicLong.incrementAndGet());
//    }
}
