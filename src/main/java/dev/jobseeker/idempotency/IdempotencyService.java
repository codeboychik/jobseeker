package dev.jobseeker.idempotency;

import dev.jobseeker.infrastructure.json.JsonCodec;
import dev.jobseeker.shared.error.ApplicationException;
import java.util.HashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {
    private final IdempotencyRepository repository;
    private final JsonCodec json;

    public IdempotencyService(IdempotencyRepository repository, JsonCodec json) {
        this.repository = repository;
        this.json = json;
    }

    @Transactional
    public IdempotentResponse execute(
            String key, String route, Object payload, Supplier<IdempotentResponse> action) {
        if (key == null || key.isBlank() || key.length() > 128)
            throw ApplicationException.field(
                    "Idempotency-Key",
                    "Supply a unique key of at most 128 characters for POST requests.");
        String scope = "POST " + route + " " + key, hash = json.hash(payload);
        // Serialize concurrent retries before lookup, mutation and response persistence.
        repository.lock(scope);
        var existing = repository.find(scope);
        if (!existing.isEmpty()) {
            var row = existing.getFirst();
            if (!hash.equals(row.get("requestHash")))
                throw new ApplicationException(
                        409, "This idempotency key was already used with different content.");
            return new IdempotentResponse(
                    ((Number) row.get("responseStatus")).intValue(),
                    json.object(row.get("responseBody").toString()),
                    (String) row.get("location"));
        }
        IdempotentResponse result = action.get();
        var values = new HashMap<String, Object>();
        values.put("scope", scope);
        values.put("request_hash", hash);
        values.put("response_status", result.status());
        values.put("response_body", json.write(result.body()));
        values.put("location", result.location());
        repository.insert(values);
        return result;
    }
}
