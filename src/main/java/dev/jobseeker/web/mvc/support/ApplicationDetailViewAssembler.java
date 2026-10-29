package dev.jobseeker.web.mvc.support;

import dev.jobseeker.application.ApplicationDetailService;
import dev.jobseeker.application.ApplicationFields;
import dev.jobseeker.application.ApplicationStatus;
import dev.jobseeker.application.SalaryBasis;
import dev.jobseeker.application.SalaryPeriod;
import dev.jobseeker.catalog.LookupKind;
import dev.jobseeker.catalog.LookupTypeService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
public class ApplicationDetailViewAssembler {
    private final ApplicationDetailService details;
    private final LookupTypeService lookups;
    private final FormValues forms;

    public ApplicationDetailViewAssembler(
            ApplicationDetailService details, LookupTypeService lookups, FormValues forms) {
        this.details = details;
        this.lookups = lookups;
        this.forms = forms;
    }

    public ModelAndView render(UUID id, String returnTo) {
        var view = new ModelAndView("applications/detail");
        var application = id == null ? newApplication() : details.detail(id);
        view.addObject("app", application);
        view.addObject("creating", id == null);
        view.addObject("returnTo", ReturnLocation.sanitize(returnTo));
        view.addObject("saved", false);
        view.addObject("statuses", ApplicationStatus.codes());
        view.addObject("periods", SalaryPeriod.codes());
        view.addObject("bases", SalaryBasis.codes());
        view.addObject("roundTypes", lookups.list(LookupKind.ROUND));
        view.addObject("reasonTypes", lookups.list(LookupKind.REJECTION_REASON));
        if (id != null) addReferenceOptions(view, application);
        return view;
    }

    private Map<String, Object> newApplication() {
        var application = new LinkedHashMap<String, Object>();
        ApplicationFields.FIELDS.keySet().forEach(key -> application.put(key, null));
        application.put("status", "SAVED");
        return application;
    }

    private void addReferenceOptions(ModelAndView view, Map<String, Object> application) {
        var rounds = children(application, "rounds");
        var notes = children(application, "notes");
        var history = children(application, "statusChanges");
        var questions =
                rounds.stream().flatMap(round -> children(round, "questions").stream()).toList();
        var rejections =
                history.stream()
                        .filter(event -> "REJECTED".equals(event.get("newStatus")))
                        .toList();
        view.addObject(
                "roundRefs",
                options(
                        rounds,
                        round ->
                                round.get("typeName")
                                        + " — "
                                        + Objects.toString(round.get("title"), "Untitled round")));
        view.addObject("noteRefs", options(notes, note -> abbreviate(note.get("text"))));
        view.addObject(
                "questionRefs",
                options(questions, question -> abbreviate(question.get("question"))));
        view.addObject(
                "rejectionRefs",
                options(
                        rejections,
                        event ->
                                "Rejection #"
                                        + event.get("sequence")
                                        + " — "
                                        + forms.time(event.get("eventTime"))));
        view.addObject(
                "currentRejectionId",
                "REJECTED".equals(application.get("status")) ? history.getFirst().get("id") : null);
    }

    private List<SelectOption> options(
            List<Map<String, Object>> rows, Function<Map<String, Object>, String> label) {
        return rows.stream()
                .map(row -> new SelectOption((UUID) row.get("id"), label.apply(row)))
                .toList();
    }

    private String abbreviate(Object value) {
        String text = value.toString();
        return text.substring(0, Math.min(90, text.length()));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> children(Map<String, Object> parent, String name) {
        return (List<Map<String, Object>>) parent.get(name);
    }
}
