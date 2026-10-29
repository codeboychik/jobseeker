package dev.jobseeker.web.mvc;

import dev.jobseeker.application.ApplicationStatus;
import dev.jobseeker.search.ApplicationFilter;
import dev.jobseeker.search.ApplicationSearchService;
import dev.jobseeker.statistics.StatisticsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class DashboardController {
    private final ApplicationSearchService search;
    private final StatisticsService statistics;

    public DashboardController(ApplicationSearchService search, StatisticsService statistics) {
        this.search = search;
        this.statistics = statistics;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/applications";
    }

    @GetMapping("/applications")
    public ModelAndView dashboard(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "active") String archive,
            @RequestParam(defaultValue = "") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page) {
        var filter = new ApplicationFilter(q, status, archive, sort, direction, page, 25);
        var view = new ModelAndView("applications/dashboard");
        view.addObject("filter", filter);
        view.addObject("page", search.list(filter));
        view.addObject("summary", statistics.summary(filter));
        view.addObject("statuses", ApplicationStatus.codes());
        var base =
                UriComponentsBuilder.fromPath("/applications")
                        .queryParam("q", q)
                        .queryParam("status", status)
                        .queryParam("archive", archive)
                        .queryParam("sort", sort)
                        .queryParam("direction", direction);
        view.addObject(
                "returnTo",
                base.cloneBuilder().queryParam("page", page).build().encode().toUriString());
        view.addObject(
                "previousUrl",
                base.cloneBuilder()
                        .queryParam("page", Math.max(0, page - 1))
                        .build()
                        .encode()
                        .toUriString());
        view.addObject(
                "nextUrl",
                base.cloneBuilder().queryParam("page", page + 1).build().encode().toUriString());
        return view;
    }
}
