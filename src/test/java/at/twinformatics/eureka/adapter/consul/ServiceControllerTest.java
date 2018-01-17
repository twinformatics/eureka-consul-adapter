/**
 * The MIT License
 * Copyright © 2018 Twinformatics GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package at.twinformatics.eureka.adapter.consul;

import at.twinformatics.eureka.adapter.consul.event.ServiceChangeDetector;
import at.twinformatics.eureka.adapter.consul.controller.ServiceController;
import at.twinformatics.eureka.adapter.consul.mapper.ServiceMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
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
    }

    @Test
    public void getNoServices() throws Exception {

        when(registry.getApplications()).thenReturn(new Applications());

        this.mockMvc.perform(get("/v1/catalog/services?wait=1ms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(content().string("{}"));
    }

    @Test
    public void getServices() throws Exception {

        mock2Applications();

        this.mockMvc.perform(get("/v1/catalog/services?wait=1ms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.ms1", is(new JSONArray())))
                .andExpect(jsonPath("$.ms2", is(new JSONArray())));
    }

    @Test
    public void getServicesTimeout() throws Exception {

        mock2Applications();

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

        mock2Applications();

        new Thread(() -> {
            sleepFor(1000);
            serviceChangeDetector.publish("ms1", 2);
            sleepFor(1000);
            serviceChangeDetector.publish("ms1", 3);
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

    @Test(timeout = 10000)
    public void get2ServicesTimeoutWithChange() throws Exception {

        mock2Applications();

        new Thread(() -> {
            sleepFor(1000);
            serviceChangeDetector.publish("ms1", 2);
            sleepFor(1000);

            Applications applications = new Applications();
            applications.addApplication(new Application("ms1"));
            when(registry.getApplications()).thenReturn(applications);

            serviceChangeDetector.publish("ms2", 4);
            sleepFor(1000);
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

        this.mockMvc.perform(get("/v1/catalog/services?wait=30s&index=2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(header().string("X-Consul-Index", "4"))
                .andExpect(jsonPath("$.ms1", is(new JSONArray())))
                .andExpect(jsonPath("$.ms2").doesNotExist());

    }

    @Test
    public void getService() throws Exception {

        mock2Applications();
        Application ms1 = registry.getApplications().getRegisteredApplications().get(0);

        InstanceInfo instance1 = mock(InstanceInfo.class);
        when(instance1.getId()).thenReturn("1");
        when(instance1.getAppName()).thenReturn("ms1");
        when(instance1.getIPAddr()).thenReturn("1.2.3.4");
        when(instance1.getPort()).thenReturn(80);
        when(instance1.isPortEnabled(InstanceInfo.PortType.SECURE)).thenReturn(false);
        when(instance1.getSecurePort()).thenReturn(443);

        ms1.addInstance(instance1);

        when(registry.getApplication("ms1")).thenReturn(ms1);

        this.mockMvc.perform(get("/v1/catalog/service/ms1?wait=1ms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].Address", is("1.2.3.4")))
                .andExpect(jsonPath("$[0].Node", is("ms1")))
                .andExpect(jsonPath("$[0].ServiceAddress", is("1.2.3.4")))
                .andExpect(jsonPath("$[0].ServiceID", is("1")))
                .andExpect(jsonPath("$[0].ServicePort", is(80)))
                .andExpect(jsonPath("$[0].NodeMeta").isEmpty())
                .andExpect(jsonPath("$[0].ServiceTags", is(new JSONArray())));

        InstanceInfo instance2 = mock(InstanceInfo.class);
        when(instance2.getId()).thenReturn("2");
        when(instance2.getAppName()).thenReturn("ms1");
        when(instance2.getIPAddr()).thenReturn("1.2.3.5");
        when(instance2.getPort()).thenReturn(81);
        when(instance2.isPortEnabled(InstanceInfo.PortType.SECURE)).thenReturn(true);
        when(instance2.getSecurePort()).thenReturn(443);

        Map<String, String> md = new HashMap<>();
        md.put("k1", "v1");
        md.put("k2", "v2");
        when(instance2.getMetadata()).thenReturn(md);

        ms1.addInstance(instance2);

        this.mockMvc.perform(get("/v1/catalog/service/ms1?wait=1ms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].Address", is("1.2.3.4")))
                .andExpect(jsonPath("$[0].Node", is("ms1")))
                .andExpect(jsonPath("$[0].ServiceAddress", is("1.2.3.4")))
                .andExpect(jsonPath("$[0].ServiceID", is("1")))
                .andExpect(jsonPath("$[0].ServicePort", is(80)))
                .andExpect(jsonPath("$[0].NodeMeta").isEmpty())
                .andExpect(jsonPath("$[0].ServiceTags", is(new JSONArray())))

                .andExpect(jsonPath("$[1].Address", is("1.2.3.5")))
                .andExpect(jsonPath("$[1].Node", is("ms1")))
                .andExpect(jsonPath("$[1].ServiceAddress", is("1.2.3.5")))
                .andExpect(jsonPath("$[1].ServiceID", is("2")))
                .andExpect(jsonPath("$[1].ServicePort", is(443)))
                .andExpect(jsonPath("$[1].NodeMeta.k1", is("v1")))
                .andExpect(jsonPath("$[1].NodeMeta.k2", is("v2")))
                .andExpect(jsonPath("$[1].ServiceTags", is(new JSONArray())));
    }

    @Test(timeout = 10000)
    public void getServiceTimeoutWithChange() throws Exception {

        mock2Applications();
        Application ms1 = registry.getApplications().getRegisteredApplications().get(0);

        InstanceInfo instance1 = mock(InstanceInfo.class);
        when(instance1.getId()).thenReturn("1");
        when(instance1.getAppName()).thenReturn("ms1");
        when(instance1.getIPAddr()).thenReturn("1.2.3.4");
        when(instance1.getPort()).thenReturn(80);
        when(instance1.isPortEnabled(InstanceInfo.PortType.SECURE)).thenReturn(false);
        when(instance1.getSecurePort()).thenReturn(443);

        ms1.addInstance(instance1);

        when(registry.getApplication("ms1")).thenReturn(ms1);

        new Thread(() -> {
            sleepFor(1000);
            serviceChangeDetector.publish("ms1", 2);
            sleepFor(500);
            serviceChangeDetector.publish("ms2", 1);
            serviceChangeDetector.publish("ms3", 1);
            serviceChangeDetector.publish("ms4", 1);
            sleepFor(500);
            when(instance1.getIPAddr()).thenReturn("8.8.8.8");
            serviceChangeDetector.publish("ms1", 3);
        }).start();

        this.mockMvc.perform(get("/v1/catalog/service/ms1?wait=30s"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(header().string("X-Consul-Index", "1"))
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].Address", is("1.2.3.4")));

        this.mockMvc.perform(get("/v1/catalog/service/ms1?wait=30s&index=1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(header().string("X-Consul-Index", "2"))
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].Address", is("1.2.3.4")));


        this.mockMvc.perform(get("/v1/catalog/service/ms1?wait=30s&index=2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(header().string("X-Consul-Index", "3"))
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].Address", is("8.8.8.8")));

    }

    private void mock2Applications() {
        Applications applications = new Applications();
        Application app1 = new Application();
        app1.setName("ms1");
        applications.addApplication(app1);
        Application app2 = new Application();
        app2.setName("ms2");
        applications.addApplication(app2);

        when(registry.getApplications()).thenReturn(applications);
    }

    private void sleepFor(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            fail();
        }
    }
}
