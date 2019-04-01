package at.twinformatics.eureka.adapter.consul.mapper;

import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "eurekaConsulAdapter.useNodeMeta", havingValue = "false",
                       matchIfMissing = true)
@Component
public class ServiceMetadataMapper implements MetadataMapper {

    @Override
    public Map<String, String> extractNodeMetadata(@NonNull Map<String, String> instanceMetaData) {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> extractServiceMetadata(@NonNull Map<String, String> instanceMetaData) {
        return instanceMetaData;
    }
}
