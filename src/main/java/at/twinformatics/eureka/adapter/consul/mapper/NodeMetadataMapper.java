package at.twinformatics.eureka.adapter.consul.mapper;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "eurekaConsulAdapter.useNodeMeta", havingValue = "true")
@Component
public class NodeMetadataMapper implements MetadataMapper {

    private final String nodeMetaPrefix;
    private Pattern nodeMetaPattern;

    public NodeMetadataMapper(String nodeMetaPrefix) {
        this.nodeMetaPrefix = nodeMetaPrefix;
        nodeMetaPattern = Pattern.compile(String.format("^%s.+$", nodeMetaPrefix));
    }

    @Override
    public Map<String, String> extractNodeMetadata(@NonNull Map<String, String> instanceMetaData) {
        return instanceMetaData.entrySet().stream()
                               .filter(entry -> nodeMetaPattern.matcher(entry.getKey()).matches())
                               .collect(Collectors.toMap(
                                   entry -> entry.getKey().substring(nodeMetaPrefix.length())
                                   , Map.Entry::getValue
                                   , (oldValue, newValue) -> newValue
                               ));
    }

    @Override
    public Map<String, String> extractServiceMetadata(
        @NonNull Map<String, String> instanceMetaData) {
        return instanceMetaData.entrySet().stream()
                               .filter(e -> !nodeMetaPattern.matcher(e.getKey()).matches())
                               .collect(Collectors.toMap(
                                   Map.Entry::getKey
                                   , Map.Entry::getValue
                                   , (oldValue, newValue) -> newValue)
                               );
    }
}
