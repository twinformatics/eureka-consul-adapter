package at.twinformatics.eureka_consul_adapter.mapper;

import at.twinformatics.eureka_consul_adapter.model.Service;
import com.netflix.appinfo.InstanceInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.netflix.appinfo.InstanceInfo.PortType.SECURE;

@Component
public class ServiceMapper {

    private static final List<String> NO_SERVICE_TAGS = new ArrayList<>();

    public Service map(InstanceInfo instanceInfo) {
        return Service.builder()
                .address(instanceInfo.getIPAddr())
                .serviceAddress(instanceInfo.getIPAddr())
                .serviceID(instanceInfo.getId())
                .servicePort(getPort(instanceInfo))
                .node(instanceInfo.getAppName())
                .nodeMeta(new HashMap<>(instanceInfo.getMetadata()))
                .serviceTags(NO_SERVICE_TAGS)
                .build();
    }

    private int getPort(InstanceInfo instanceInfo) {
        if (instanceInfo.isPortEnabled(SECURE)) {
            return instanceInfo.getSecurePort();
        } else {
            return instanceInfo.getPort();
        }
    }
}
