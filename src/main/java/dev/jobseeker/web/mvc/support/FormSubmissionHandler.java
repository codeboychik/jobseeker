package dev.jobseeker.web.mvc.support;

import dev.jobseeker.shared.error.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
public class FormSubmissionHandler {
    private final FormRequestParser parser;
    private final ApplicationDetailViewAssembler views;

    public FormSubmissionHandler(FormRequestParser parser, ApplicationDetailViewAssembler views) {
        this.parser = parser;
        this.views = views;
    }

    private ModelAndView failure(
            UUID id,
            String back,
            String form,
            Map<String, String> fields,
            ApplicationException error,
            HttpServletRequest request) {
        request.setAttribute("failedForm", form);
        request.setAttribute("submitted", fields);
        var view = views.render(id, back);
        view.setStatus(org.springframework.http.HttpStatus.valueOf(error.status()));
        view.addObject("error", error.getMessage());
        view.addObject("fieldErrors", error.fields());
        if (error.current() != null) view.addObject("currentValues", error.current());
        return view;
    }

    private ModelAndView redirect(UUID id, String back, String anchor) {
        return new ModelAndView(
                "redirect:/applications/"
                        + id
                        + "?returnTo="
                        + URLEncoder.encode(back, StandardCharsets.UTF_8)
                        + "&saved=true#"
                        + anchor);
    }

    public ModelAndView submit(
            UUID id, Map<String, String> fields, HttpServletRequest request, FormAction action)
            throws IOException {
        String back = ReturnLocation.sanitize(fields.get("returnTo"));
        String form = fields.getOrDefault("formId", "overview");
        try {
            FormResult result = action.execute(parser.parse(fields));
            return redirect(result.applicationId(), back, result.anchor());
        } catch (ApplicationException e) {
            return failure(id, back, form, fields, e, request);
        } catch (DataIntegrityViolationException e) {
            return failure(
                    id,
                    back,
                    form,
                    fields,
                    new ApplicationException(
                            409,
                            "This name or reference conflicts with an existing record. Check the"
                                    + " current values."),
                    request);
        }
    }
}
