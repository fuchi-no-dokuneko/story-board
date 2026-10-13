package dev.storyblock.api.http;

import dev.storyblock.application.*;
import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.domain.*;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HiddenVersionHistoryTest {
    @TempDir Path root;
    @Test void editedNotesRestoreAndTransferWithTheirCompleteHistory() throws Exception {
        var base = HiddenHttpFixture.revision(); var scene = base.novel().chapters().getFirst().scenes().getFirst();
        var old = scene.blocks().get(1); String changedText = "New symbolism\nPayoff moved to chapter 8, scene 3.";
        try (var source = SqliteRevisionStore.open(root.resolve("source.db"));
             var target = SqliteRevisionStore.open(root.resolve("target.db"))) {
            source.createNovel(base,CacheRevisionFixture.hash(base));
            var replace = new EditOperation.ReplaceBlockRange(context(base,"replace"),
                    BlockRangeGuard.capture(scene,old.id(),old.id()),List.of(new BlockDraft(old.id(),changedText,
                    old.metadata(),old.extensions())));
            var commits = new CommitService(source); var changedId = Ids.RevisionId.create();
            commits.commit(replace,changedId,base.createdAt().plusSeconds(1));
            var changed = source.getRevision(base.novel().id(),changedId).manifest();
            assertNotEquals(old.versionId(),changed.requireBlock(old.id()).versionId());
            assertTrue(changed.requireBlock(old.id()).isHidden());
            var restoreId = Ids.RevisionId.create();
            commits.commit(new EditOperation.RestoreRevisionContent(context(changed,"restore"),base.id(),CacheRevisionFixture.hash(base)),
                    restoreId,base.createdAt().plusSeconds(2));
            assertEquals(old,source.getRevision(base.novel().id(),restoreId).manifest().requireBlock(old.id()));
            var bytes = new CanonicalTransferService(source).exportPackage(base.novel().id());
            String json = new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(json.contains(HiddenHttpFixture.SENTINEL));assertTrue(json.contains(changedText.split("\n")[0]));
            new CanonicalTransferService(target).importDocument(CanonicalExportFormat.PACKAGE,bytes,"history",base.createdAt());
            assertEquals(changed,target.getRevision(base.novel().id(),changedId).manifest());
            assertEquals(base,target.getRevision(base.novel().id(),base.id()).manifest());
            assertArrayEquals(bytes,new CanonicalTransferService(target).exportPackage(base.novel().id()));
        }
    }
    private EditContext context(RevisionManifest revision,String key) {
        return new EditContext(Ids.OperationId.create(),key,revision.novel().id(),revision.id(),CacheRevisionFixture.hash(revision));
    }
}
