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
package at.twinformatics.eureka.adapter.consul.mapper;

import at.twinformatics.eureka.adapter.consul.model.Service;
import com.netflix.appinfo.InstanceInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.netflix.appinfo.InstanceInfo.PortType.SECURE;

@Component
public class InstanceInfoMapper {

    private static final List<String> NO_SERVICE_TAGS = new ArrayList<>();

    @Value("${eurekaConsulAdapter.preferHostName:false}")
    private boolean preferHostName;
    
    public Service map(InstanceInfo instanceInfo) {
        String address = getAddress(instanceInfo);
        return Service.builder()
                .address(address)
                .serviceAddress(address)
                .serviceID(instanceInfo.getId())
                .servicePort(getPort(instanceInfo))
                .node(instanceInfo.getAppName())
                .nodeMeta(new HashMap<>(instanceInfo.getMetadata()))
                .serviceTags(NO_SERVICE_TAGS)
                .build();
    }
    
    private String getAddress(InstanceInfo instanceInfo) {
        if (preferHostName) {
            return instanceInfo.getHostName();
        } else {
            return instanceInfo.getIPAddr();
        }
    }

    private int getPort(InstanceInfo instanceInfo) {
        if (instanceInfo.isPortEnabled(SECURE)) {
            return instanceInfo.getSecurePort();
        } else {
            return instanceInfo.getPort();
        }
    }

    public void setPreferHostName(boolean preferHostName) {
        this.preferHostName = preferHostName;
    }
}
