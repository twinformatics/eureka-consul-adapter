# Eureka Consul Adapter _(for Prometheus)_

This project contains a Spring Boot Starter that registers HTTP endpoints on a [Spring Cloud Eureka server](https://cloud.spring.io/spring-cloud-netflix/) to support [Prometheus](https://prometheus.io/)'s 
[discovery mechanism for Consul (<consul_sd_config>)](https://prometheus.io/docs/prometheus/latest/configuration/configuration/#<consul_sd_config>)

# Restrictions
This adapter _does not support everything_ of [Consul's HTTP API](https://www.consul.io/api/index.html) 
but only those endpoints and attributes that a required for Prometheus' service discovery mechanism.

# Functionality

This starter add the following HTTP endpoints:
- `/v1/agent/self` Returns the name of the datacenter in use (Consul API: https://www.consul.io/api/agent.html#read-configuration).
In Eureka, this can be set using the `archaius.deployment.datacenter` configuration property.
- `/v1/catalog/services` Returns the names of the deployed applications (Consul API: https://www.consul.io/api/catalog.html#list-services). 
No service tags service will be returned as Eureka does not support this concept.
- `/v1/catalog/service/{service}` Returns all available details for the particular application 
(instances, host names, ports, meta data, service tags). No service tags service will be returned as Eureka does not support this concept.

# How to use this starter

Simply add this dependency the your Spring Cloud Eureka Server (https://github.com/spring-cloud-samples/eureka):

## Maven
```
<dependency>
  <groupId>at.twinformatics</groupId>
  <artifactId>eureka-consul-adapter</artifactId>
  <version>${eureka-consul-adapter.version}<version>
</dependency>
```

## Gradle
```
dependencies {
    compile 'at.twinformatics:eureka-consul-adapter:${eureka-consul-adapter.version}'
}
```

# Requirements

- Java 1.8+
- Spring Boot 1.5 (earlier version might work)
- Spring Cloud Edgware (earlier version might work)