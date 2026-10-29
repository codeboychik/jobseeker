package dev.jobseeker.search;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchIndexService {
    private final SearchIndexRepository repository;
    private final SearchDocumentBuilder documents;

    public SearchIndexService(SearchIndexRepository repository, SearchDocumentBuilder documents) {
        this.repository = repository;
        this.documents = documents;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void refresh(UUID id) {
        repository.save(documents.build(id));
    }

    @Transactional
    public int rebuild() {
        var ids = repository.lockApplicationIds();
        ids.forEach(this::refresh);
        return ids.size();
    }
}
