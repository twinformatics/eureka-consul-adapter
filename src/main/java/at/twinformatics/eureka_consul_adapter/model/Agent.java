package at.twinformatics.eureka_consul_adapter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Agent {

    @JsonProperty("Config")
    private Config config;

}
