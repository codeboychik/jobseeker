package dev.jobseeker.search;

import dev.jobseeker.application.ApplicationStatus;
import dev.jobseeker.shared.error.ApplicationException;
import java.util.List;
import java.util.Objects;

public record ApplicationFilter(
        String q,
        String status,
        String archive,
        String sort,
        String direction,
        int page,
        int size) {
    public ApplicationFilter {
        q = Objects.toString(q, "").strip();
        status = Objects.toString(status, "");
        archive = Objects.toString(archive, "active");
        sort = Objects.toString(sort, "");
        direction = Objects.toString(direction, "asc");
        if (q.length() > 500) throw ApplicationException.field("q", "Use at most 500 characters.");
        if (!status.isEmpty() && !ApplicationStatus.codes().contains(status))
            throw ApplicationException.field("status", "Unknown application status.");
        if (!List.of("active", "archived", "all").contains(archive))
            throw ApplicationException.field("archive", "Choose active, archived, or all.");
        if (!List.of("", "company", "role", "postedDate", "appliedDate", "status").contains(sort))
            throw ApplicationException.field("sort", "Unknown sort field.");
        if (!List.of("asc", "desc").contains(direction) || page < 0 || size < 1 || size > 100)
            throw new ApplicationException(400, "Invalid pagination or sorting.");
    }
}
