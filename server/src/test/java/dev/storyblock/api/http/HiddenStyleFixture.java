package dev.storyblock.api.http;

import dev.storyblock.application.StyleProfileService;
import dev.storyblock.domain.*;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import dev.storyblock.style.*;
import java.math.BigDecimal;
import java.util.*;

final class HiddenStyleFixture {
    static StyleProfileVersionView ready(SqliteRevisionStore store,RevisionManifest revision) {
        var scope = new StyleProfileScope(revision.novel().id(),StyleScopeKind.NOVEL,null);
        var audit = AuditContext.system("hidden-style",revision.createdAt());
        var service = new StyleProfileService(store);
        var profile = service.createProfile("Reference",scope,"Authored reference fixture","profile",audit).profile();
        var lexicon = StyleMaskingLexicon.empty();
        var features = new StyleFeatureAnalyzer().extract(List.of(reference()),lexicon,
                StyleFeatureContract.defaults(lexicon.vocabularyHash()));
        var windows = StyleWindowConfiguration.defaults();
        var calibration = new StyleCalibrationProfile(StyleModule.CALIBRATION_SCHEMA_VERSION,
                features.sourceHash(),features.contract().contractHash(),windows.configurationHash(),
                List.of(new StyleStratumCalibration(StyleStratum.narration(),30,
                StyleFeatureChannel.requiredChannels().stream().sorted(Comparator.comparing(Enum::ordinal))
                        .map(channel -> StyleChannelCalibration.fromDistances(channel,
                                Collections.nCopies(30,new BigDecimal("0.00001")))).toList())));
        var content = new StyleProfileVersionContent(scope,List.of(corpusSource()),features,windows,calibration.canonicalValue());
        var view = service.createVersion(profile.profileId(),content,profile.resourceHash(),"version",audit).view();
        for (var state : List.of(StyleProfileState.CALIBRATING,StyleProfileState.READY)) {
            view = service.transition(profile.profileId(),view.profileVersion().versionId(),state,
                    "Reference fixture",false,view.statusHash(),state.name(),audit).view();
        }
        return view;
    }

    static NarrativeBlock reference() {
        return new NarrativeBlock(new Ids.BlockId("blk_Ref01"),new Ids.BlockVersionId("blv_Ref01"),
                OrderKey.initial(),"The swift fox crossed a quiet meadow.",BlockMetadata.empty(),Map.of());
    }

    static StyleCorpusSource corpusSource() {
        return new StyleCorpusSource("hidden-reference",StyleFeatureAnalyzer.sourceHash(List.of(reference())),
                StyleCorpusSourceKind.OWNER,"Authored reference","test-only","fixture");
    }
}
