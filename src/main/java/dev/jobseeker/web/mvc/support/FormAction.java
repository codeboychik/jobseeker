package dev.jobseeker.web.mvc.support;

import java.io.IOException;

@FunctionalInterface
public interface FormAction {
    FormResult execute(FormSubmission submission) throws IOException;
}
