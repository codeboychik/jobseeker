package dev.jobseeker.cv;

public record ValidatedCv(
        String filename, String mediaType, long size, String checksum, byte[] bytes) {}
