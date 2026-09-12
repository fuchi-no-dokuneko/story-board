package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.MissingExportJobException;
import java.util.regex.Matcher;

final class NovelBoundaryFilterResolveNovelAction {
    static Ids.NovelId resolveNovel(NovelBoundaryFilter self, String path)  {
        Matcher novel = NovelBoundaryFilter.NOVEL_PATH.matcher(path);
        if (novel.matches()) {
            return new Ids.NovelId(novel.group(1));
        }
        Matcher job = NovelBoundaryFilter.JOB_PATH.matcher(path);
        if (job.matches()) {
            Ids.JobId id = new Ids.JobId(job.group(1));
            try {
                return self.transfers.getExportJob(id).novelId();
            } catch (MissingExportJobException missingExport) {
                return self.analyses.getJob(id).snapshot().novelId();
            }
        }
        Matcher artifact = NovelBoundaryFilter.ARTIFACT_PATH.matcher(path);
        if (artifact.matches()) {
            return self.transfers.getArtifact(new Ids.ArtifactId(artifact.group(1))).novelId();
        }
        Matcher internalResult = NovelBoundaryFilter.INTERNAL_JOB_RESULT_PATH.matcher(path);
        if (internalResult.matches()) {
            return self.analyses.getJob(
                    new Ids.JobId(internalResult.group(1))
            ).snapshot().novelId();
        }
        Matcher key = NovelBoundaryFilter.KEY_PATH.matcher(path);
        if (key.matches()) {
            return self.accessKeys.requireKey(new Ids.AccessKeyId(key.group(1))).novelId();
        }
        Matcher analysis = NovelBoundaryFilter.ANALYSIS_PATH.matcher(path);
        if (analysis.matches()) {
            return self.analyses.getAnalysis(
                    new Ids.StyleAnalysisId(analysis.group(1))
            ).snapshot().novelId();
        }
        return null;
    }
}
