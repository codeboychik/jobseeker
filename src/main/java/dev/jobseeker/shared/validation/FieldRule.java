package dev.jobseeker.shared.validation;

import java.util.function.Function;

public record FieldRule(boolean required, Function<Object, Object> parse) {}
