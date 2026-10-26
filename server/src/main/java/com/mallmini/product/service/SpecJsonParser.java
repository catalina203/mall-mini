package com.mallmini.product.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpecJsonParser {

    private final ObjectMapper objectMapper;

    public Map<String, String> parse(String specJson) {
        if (specJson == null || specJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> spec = objectMapper.readValue(
                    specJson, new TypeReference<LinkedHashMap<String, String>>() {
                    });
            return spec.entrySet().stream()
                    .filter(entry -> Objects.nonNull(entry.getKey()) && Objects.nonNull(entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (left, right) -> right, LinkedHashMap::new));
        } catch (Exception ex) {
            log.warn("cannot parse sku spec json: {}", ex.getMessage());
            return Map.of();
        }
    }
}
