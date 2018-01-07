package at.twinformatics.eureka_consul_adapter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * model details see https://www.consul.io/api/catalog.html#serviceport
 */
@Getter
@Builder
public class Service {

    @JsonProperty("Address")
    private String address;

    @JsonProperty("Node")
    private String node;

    @JsonProperty("ServiceAddress")
    private String serviceAddress;

    @JsonProperty("ServiceID")
    private String serviceID;

    @JsonProperty("ServicePort")
    private int servicePort;

    @JsonProperty("NodeMeta")
    private Map<String, String> nodeMeta;

    // will be empty, eureka does not have the concept of service tags
    @JsonProperty("ServiceTags")
    private List<String> serviceTags;

}
