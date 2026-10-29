package dev.jobseeker.search;

import java.util.List;
import java.util.Map;

public record SearchRows(List<Map<String, Object>> items, long total) {}
