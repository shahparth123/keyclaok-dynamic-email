package com.parthshah.keycloak.dynamicemail;

import java.util.AbstractMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class FlattenMap {
    private static final Logger LOG = Logger.getLogger(FlattenMap.class.getName());

    public static Stream<Map.Entry<String, Object>> flatten(Map.Entry<String, Object> entry) {
        //LOG.info(entry.getKey());
        if(entry.getValue()==null){
            entry.setValue("");
        }
        if (entry.getValue() instanceof Map<?, ?>) {
            Map<String, Object> nested = (Map<String, Object>) entry.getValue();

            return nested.entrySet().stream()
                    .map(e -> new AbstractMap.SimpleEntry(entry.getKey() + "." + e.getKey(), e.getValue()))
                    .flatMap(FlattenMap::flatten);
        }
        return Stream.of(entry);
    }
}