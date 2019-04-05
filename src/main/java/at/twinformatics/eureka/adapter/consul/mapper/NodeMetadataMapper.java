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

import lombok.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
