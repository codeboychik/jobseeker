package dev.jobseeker.infrastructure.json;

import dev.jobseeker.shared.crypto.Checksums;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JsonCodec {
    private final JsonMapper mapper = JsonMapper.builder().build();

    public String write(Object value) {
        return mapper.writeValueAsString(value);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> object(String value) {
        return mapper.readValue(value, Map.class);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> list(String value) {
        return mapper.readValue(value, List.class);
    }

    public String hash(Object value) {
        return Checksums.sha256(write(sorted(value)).getBytes(StandardCharsets.UTF_8));
    }

    private Object sorted(Object value) {
        if (value instanceof Map<?, ?> map) {
            var result = new TreeMap<String, Object>();
            map.forEach((k, v) -> result.put(k.toString(), sorted(v)));
            return result;
        }
        if (value instanceof Collection<?> list) return list.stream().map(this::sorted).toList();
        return value;
    }
}
