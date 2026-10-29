package dev.jobseeker.web.mvc.support;

import java.util.Map;
import java.util.UUID;

public record FormSubmission(Map<String, Object> values, UUID recordId, UUID roundId) {}
