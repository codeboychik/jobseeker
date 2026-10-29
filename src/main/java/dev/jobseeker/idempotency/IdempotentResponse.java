package dev.jobseeker.idempotency;

import java.util.Map;

public record IdempotentResponse(int status, Map<String, Object> body, String location) {}
