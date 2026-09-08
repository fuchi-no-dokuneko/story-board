package dev.storyblock.api.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;

final class NovelBoundaryFilterRejectAction {
    static void reject(NovelBoundaryFilter self, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (self.hideCrossNovel) {
            self.problemWriter.write(request, response, ApiFailureException.of(
                    HttpStatus.NOT_FOUND,
                    "RESOURCE_NOT_FOUND",
                    "Resource not found",
                    "resource-not-found",
                    "The requested resource does not exist."
            ));
        } else {
            self.problemWriter.write(request, response, ApiFailureException.of(
                    HttpStatus.FORBIDDEN,
                    "NOVEL_ACCESS_DENIED",
                    "Novel access denied",
                    "novel-access-denied",
                    "The credential cannot access the requested novel."
            ));
        }
    }
}
