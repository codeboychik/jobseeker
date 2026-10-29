package dev.jobseeker.search;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SearchDocument(
        UUID applicationId, String text, List<Map<String, Object>> sections, long version) {}
