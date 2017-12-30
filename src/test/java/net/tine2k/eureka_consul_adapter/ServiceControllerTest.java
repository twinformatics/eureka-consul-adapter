package net.tine2k.eureka_consul_adapter;

import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import net.minidev.json.JSONArray;
import net.tine2k.eureka_consul_adapter.controller.ServiceController;
import net.tine2k.eureka_consul_adapter.event.ServiceChangeDetector;
import net.tine2k.eureka_consul_adapter.mapper.ServiceMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {ServiceController.class, ServiceChangeDetector.class, ServiceMapper.class})
public class ServiceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PeerAwareInstanceRegistry registry;

    @Autowired
    ServiceChangeDetector serviceChangeDetector;

    @Before
    public void setUp() {
        serviceChangeDetector.reset();

        Applications applications = new Applications();
        Application app1 = new Application();
        app1.setName("ms1");
        applications.addApplication(app1);
        Application app2 = new Application();
        app2.setName("ms2");
        applications.addApplication(app2);

        when(registry.getApplications()).thenReturn(applications);
    }

    @Test
    public void getServices() throws Exception {

        this.mockMvc.perform(get("/v1/catalog/services?wait=1ms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.ms1", is(new JSONArray())))
                .andExpect(jsonPath("$.ms2", is(new JSONArray())));
    }

    @Test
    public void getServicesTimeout() throws Exception {

        this.mockMvc.perform(get("/v1/catalog/services?wait=1ms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(header().string("X-Consul-Index", "1"))
                .andExpect(jsonPath("$.ms1", is(new JSONArray())))
                .andExpect(jsonPath("$.ms2", is(new JSONArray())));

        serviceChangeDetector.publish("ms1", 2);

        this.mockMvc.perform(get("/v1/catalog/services?wait=1ms&index=1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(header().string("X-Consul-Index", "2"))
                .andExpect(jsonPath("$.ms1", is(new JSONArray())))
                .andExpect(jsonPath("$.ms2", is(new JSONArray())));

        this.mockMvc.perform(get("/v1/catalog/services?wait=1ms&index=2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(header().string("X-Consul-Index", "2"))
                .andExpect(jsonPath("$.ms1", is(new JSONArray())))
                .andExpect(jsonPath("$.ms2", is(new JSONArray())));

        serviceChangeDetector.publish("ms1", 3);

        this.mockMvc.perform(get("/v1/catalog/services?wait=1ms&index=2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(header().string("X-Consul-Index", "3"))
                .andExpect(jsonPath("$.ms1", is(new JSONArray())))
                .andExpect(jsonPath("$.ms2", is(new JSONArray())));
    }

    @Test(timeout = 10000)
    public void getServicesTimeoutWithChange() throws Exception {

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                serviceChangeDetector.publish("ms1",2);
                Thread.sleep(1000);
                serviceChangeDetector.publish("ms1", 3);
            } catch (InterruptedException e) {
                fail();
            }
        }).start();

        this.mockMvc.perform(get("/v1/catalog/services?wait=30s"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(header().string("X-Consul-Index", "1"))
                .andExpect(jsonPath("$.ms1", is(new JSONArray())))
                .andExpect(jsonPath("$.ms2", is(new JSONArray())));

        this.mockMvc.perform(get("/v1/catalog/services?wait=30s&index=1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(header().string("X-Consul-Index", "2"))
                .andExpect(jsonPath("$.ms1", is(new JSONArray())))
                .andExpect(jsonPath("$.ms2", is(new JSONArray())));

    }
}
