package at.twinformatics.eureka_consul_adapter;

import at.twinformatics.eureka_consul_adapter.controller.AgentController;
import at.twinformatics.eureka_consul_adapter.controller.ServiceController;
import at.twinformatics.eureka_consul_adapter.event.CanceledEventHandler;
import at.twinformatics.eureka_consul_adapter.event.RegisteredEventHandler;
import at.twinformatics.eureka_consul_adapter.event.ServiceChangeDetector;
import at.twinformatics.eureka_consul_adapter.mapper.ServiceMapper;
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
        return new ServiceController(peerAwareInstanceRegistry, serviceChangeDetector(), serviceMapper());
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
}
