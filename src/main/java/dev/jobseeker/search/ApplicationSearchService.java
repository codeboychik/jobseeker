package dev.jobseeker.search;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ApplicationSearchService {
    private final ApplicationSearchRepository repository;
    private final SearchExcerptBuilder excerpts;

    public ApplicationSearchService(
            ApplicationSearchRepository repository, SearchExcerptBuilder excerpts) {
        this.repository = repository;
        this.excerpts = excerpts;
    }

    public Map<String, Object> list(ApplicationFilter filter) {
        var results = repository.find(filter);
        results.items().forEach(row -> excerpts.apply(row, filter.q()));
        return Map.of(
                "items",
                results.items(),
                "total",
                results.total(),
                "page",
                filter.page(),
                "size",
                filter.size(),
                "pages",
                (results.total() + filter.size() - 1) / filter.size());
    }
}
