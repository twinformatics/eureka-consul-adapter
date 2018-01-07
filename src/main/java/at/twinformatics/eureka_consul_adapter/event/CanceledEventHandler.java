package at.twinformatics.eureka_consul_adapter.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.context.ApplicationListener;

@Slf4j
@RequiredArgsConstructor
public class CanceledEventHandler implements ApplicationListener<EurekaInstanceCanceledEvent> {

    private final ServiceChangeDetector serviceChangeDetector;

    @Override
    public void onApplicationEvent(EurekaInstanceCanceledEvent event) {

        if (log.isDebugEnabled()) {
            log.debug("Eureka Instance Canceled: " + event.getAppName());
        }
        serviceChangeDetector.publish(event.getAppName(), event.getTimestamp());
    }
}
