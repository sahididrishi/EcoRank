package dev.ecorank.backend.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards non-API, non-actuator, non-static paths to index.html
 * so the React SPA router can handle client-side routing.
 */
@Controller
public class SpaForwardingController {

    /**
     * Match any path that is NOT an API route, actuator endpoint, or static resource,
     * and forward to index.html for the React SPA.
     */
    @GetMapping(value = {
            "/{path:^(?!api|actuator|swagger-ui|v3|h2-console|static|assets|favicon\\.ico).*$}",
            "/{path:^(?!api|actuator|swagger-ui|v3|h2-console|static|assets|favicon\\.ico).*$}/**"
    })
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}
