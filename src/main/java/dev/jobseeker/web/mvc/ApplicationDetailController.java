package dev.jobseeker.web.mvc;

import dev.jobseeker.web.mvc.support.ApplicationDetailViewAssembler;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ApplicationDetailController {
    private final ApplicationDetailViewAssembler views;

    public ApplicationDetailController(ApplicationDetailViewAssembler views) {
        this.views = views;
    }

    @GetMapping("/applications/new")
    public ModelAndView create(@RequestParam(defaultValue = "/applications") String returnTo) {
        return views.render(null, returnTo);
    }

    @GetMapping("/applications/{id}")
    public ModelAndView detail(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "/applications") String returnTo,
            @RequestParam(defaultValue = "false") boolean saved) {
        return views.render(id, returnTo).addObject("saved", saved);
    }
}
