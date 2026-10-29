package dev.jobseeker.web.api;

import dev.jobseeker.search.ApplicationFilter;
import dev.jobseeker.statistics.StatisticsService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class StatisticsApiController {
    private final StatisticsService statistics;

    public StatisticsApiController(StatisticsService statistics) {
        this.statistics = statistics;
    }

    @GetMapping("/statistics/summary")
    public Map<String, Object> summary(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "active") String archive) {
        return statistics.summary(new ApplicationFilter(q, status, archive, "", "asc", 0, 25));
    }
}
