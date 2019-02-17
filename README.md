# Eureka Consul Adapter _(for Prometheus)_

This project contains a Spring Boot Starter that registers HTTP endpoints on a [Spring Cloud Eureka server](https://cloud.spring.io/spring-cloud-netflix/) to support [Prometheus](https://prometheus.io/)'s 
[discovery mechanism for Consul (<consul_sd_config>)](https://prometheus.io/docs/prometheus/latest/configuration/configuration/#<consul_sd_config>)

# Restrictions
This adapter _does not support everything_ of [Consul's HTTP API](https://www.consul.io/api/index.html) 
but only those endpoints and attributes that are required for Prometheus' service discovery mechanism.

# Functionality

This starter adds the following HTTP endpoints:
- `/v1/agent/self` Returns the name of the datacenter in use (Consul API: https://www.consul.io/api/agent.html#read-configuration).
In Eureka, this can be set using the `archaius.deployment.datacenter` configuration property.
- `/v1/catalog/services` Returns the names of the deployed applications (Consul API: https://www.consul.io/api/catalog.html#list-services). 
No service tags service will be returned as Eureka does not support this concept.
- `/v1/catalog/service/{service}` Returns all available details for the particular application 
(instances, host names, ports, meta data, service tags). No service tags service will be returned as Eureka does not support this concept.

# Long-polling

Consul HTTP API offers long-polling on some of its endpoints. Prometheus' client uses this functionality to get 
notified about changes in the registry (i.e. service de-/registrations). Prometheus adds the `wait` parameter 
(e.g. http://service-registry/v1/catalog/service/MY-SERVICE?wait=30000ms ), which causes the response
to be delayed by specified time passed or returns immediately if the service itself changes within this time range.
This behaviour has the benefit, that services changes are detected at once whithout have to wait the whole polling interval.
Prometheus opens one long-polling request for each service. In order not to block one thread for each call, this adapter
uses the async capabilities of Spring MVC. The default timeout for async requests is by default lower than 30 seconds
 and can cause `AsyncRequestTimeoutExceptions`. **To prevent this you need to set `spring.mvc.async.request-timeout` to
 at least 35000 (35 seconds)**.

# How to use this starter

Simply add this dependency the your Spring Cloud Eureka Server (https://github.com/spring-cloud-samples/eureka):

## Maven
```xml
<dependency>
  <groupId>at.twinformatics</groupId>
  <artifactId>eureka-consul-adapter</artifactId>
  <version>${eureka-consul-adapter.version}<version>
</dependency>
```

## Gradle
```groovy
dependencies {
    compile 'at.twinformatics:eureka-consul-adapter:${eureka-consul-adapter.version}'
}
```

# Requirements

- Java 1.8+

### Versions 1.1.x and later
- Spring Boot 2.1.x
- Spring Cloud Finchley

### Versions 1.0.x and later
- Spring Boot 2.0.x
- Spring Cloud Finchley

### Versions 0.x
- Spring Boot 1.5.x 
- Spring Cloud Edgware


