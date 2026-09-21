package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.storage.*;

final class ImageUploadReplay {
    static ImageUploadService.Result find(RevisionStore store, Ids.ArtifactId id,
            Ids.NovelId novel, String expectedHash, String key, byte[] content,
            ImageUploadService.ImageInfo info) {
        final StoredArtifact prior;
        try { prior = store.getArtifact(id); }
        catch (MissingArtifactException absent) { return null; }
        String hash = CanonicalJson.hashBytes(content);
        if (!prior.novelId().equals(novel) || !prior.portable()
                || !prior.kind().equals("narrative-image")
                || !prior.mediaType().equals(info.mediaType())
                || !prior.contentHash().equals(hash)
                || !store.getRevision(novel, prior.revisionId()).contentHash().equals(expectedHash)) {
            throw new IdempotencyConflictException(key, prior.contentHash(), hash);
        }
        return new ImageUploadService.Result(prior, info.widthPixels(), info.heightPixels(), true);
    }
}
