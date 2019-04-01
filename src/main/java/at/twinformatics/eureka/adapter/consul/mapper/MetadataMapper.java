package at.twinformatics.eureka.adapter.consul.mapper;

import java.util.Map;

public interface MetadataMapper {

    Map<String, String> extractNodeMetadata(Map<String, String> instanceMetaData);

    Map<String, String> extractServiceMetadata(Map<String, String> instanceMetaData);
}
