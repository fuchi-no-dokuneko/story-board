package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.RewriteModule;
import dev.storyblock.rewrite.policy.RewriteEligibilityException;
import dev.storyblock.style.StyleAnalysisWindowFinding;
import dev.storyblock.style.StyleAnalysisWindowSlice;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class RewriteGateServiceSelectFindingsAction {
    static List<StyleAnalysisWindowFinding> selectFindings(RewriteGateService self, Ids.StyleAnalysisId analysisId, List<String> findingIds)  {
        findingIds = List.copyOf(findingIds);
        if (findingIds.isEmpty() || findingIds.size() > RewriteModule.MAX_FINDINGS
                || new HashSet<>(findingIds).size() != findingIds.size()) {
            throw new RewriteEligibilityException(
                    "Rewrite finding selection must be nonempty and unique"
            );
        }
        Set<String> requested = Set.copyOf(findingIds);
        List<StyleAnalysisWindowFinding> selected = new ArrayList<>();
        int after = -1;
        while (true) {
            StyleAnalysisWindowSlice page = self.analyses.listStyleAnalysisWindows(
                    analysisId, after, 200
            );
            page.items().stream()
                    .filter(value -> requested.contains(value.windowId()))
                    .forEach(selected::add);
            if (page.nextOrdinal() == null) {
                break;
            }
            after = page.nextOrdinal();
        }
        if (selected.size() != requested.size()) {
            throw new RewriteEligibilityException(
                    "One or more selected rewrite findings do not exist"
            );
        }
        return List.copyOf(selected);
    }
}
