package at.twinformatics.eureka_consul_adapter.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.ApplicationListener;

@Slf4j
@RequiredArgsConstructor
public class RegisteredEventHandler implements ApplicationListener<EurekaInstanceRegisteredEvent> {

    private final ServiceChangeDetector serviceChangeDetector;

    @Override
    public void onApplicationEvent(EurekaInstanceRegisteredEvent event) {

        if (log.isDebugEnabled()) {
            log.debug("Eureka Instance Registered: " + event.getInstanceInfo().getAppName());
        }
        serviceChangeDetector.publish(event.getInstanceInfo().getAppName(), event.getTimestamp());
    }
}
