package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StoredArtifact;
import java.time.Instant;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

final class CanonicalTransferControllerGetArtifactAction {
    static ResponseEntity<byte[]> getArtifact(CanonicalTransferController self, String artifactId)  {
        Ids.ArtifactId id = new Ids.ArtifactId(artifactId);
        self.analyses.requireArtifactAvailable(id, Instant.now(self.clock));
        StoredArtifact artifact = self.transfers.getArtifact(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(artifact.mediaType()));
        headers.setContentLength(artifact.content().length);
        headers.set(HttpHeaders.ETAG, CanonicalTransferControllerQuotedEtag.quotedEtag(artifact.contentHash()));
        headers.setCacheControl("no-store");
        headers.set(CanonicalTransferController.ARTIFACT_CODEC_HEADER, artifact.codec());
        String extension = CanonicalTransferControllerArtifactExtension.artifactExtension(artifact);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(artifact.artifactId().value() + extension)
                .build());
        return new ResponseEntity<>(artifact.content(), headers, HttpStatus.OK);
    }
}
