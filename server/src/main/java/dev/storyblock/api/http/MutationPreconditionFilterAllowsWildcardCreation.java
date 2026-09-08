package dev.storyblock.api.http;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import static dev.storyblock.api.http.MutationPreconditionFilter.WILDCARD_CREATION_ROUTES;

final class MutationPreconditionFilterAllowsWildcardCreation {
    static boolean allowsWildcardCreation(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && WILDCARD_CREATION_ROUTES.contains(request.getRequestURI());
    }
}
