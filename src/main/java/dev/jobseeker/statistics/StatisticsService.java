package dev.jobseeker.statistics;

import dev.jobseeker.search.ApplicationFilter;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class StatisticsService {
    private final StatisticsRepository repository;

    public StatisticsService(StatisticsRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> summary(ApplicationFilter filter) {
        var result = repository.summary(filter);
        result.put(
                "definitions",
                Map.of(
                        "applications",
                        "Distinct applications matching the list filters.",
                        "currentlyRejected",
                        "Current status is REJECTED.",
                        "everRejected",
                        "Distinct applications with a recorded rejection event.",
                        "rejectionEvents",
                        "Rejection events, including repeated rejection after reopening.",
                        "reasonEntries",
                        "Active reasons linked to the current rejection event; superseded and"
                                + " unassigned concerns excluded.",
                        "reasonGroups",
                        "Distinct application counts by category and source; overlapping categories"
                                + " must not be summed.",
                        "rejectionsWithoutReason",
                        "Currently rejected applications with no active reason for their current"
                                + " rejection event."));
        return result;
    }
}
