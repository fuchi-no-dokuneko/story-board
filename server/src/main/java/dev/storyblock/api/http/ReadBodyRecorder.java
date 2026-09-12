package dev.storyblock.api.http;

import java.util.List;
import java.util.Map;

final class ReadBodyRecorder {
    private final RecentReadLedger ledger;
    ReadBodyRecorder(RecentReadLedger ledger) { this.ledger = ledger; }
    void record(Map<?, ?> value) {
        ledger.prune();
        for (String wrapper : List.of("revision", "render_packet")) {
            if (value.get(wrapper) instanceof Map<?, ?> nested) { record(nested); return; }
        }
        if (!(value.get("novel_id") instanceof String novel)
                || !(value.get("revision_id") instanceof String revision)) return;
        if (value.get("chapters") instanceof List<?> chapters) {
            for (Object raw : chapters) {
                if (raw instanceof Map<?, ?> chapter && chapter.get("scenes") instanceof List<?> scenes) {
                    for (Object item : scenes) {
                        if (item instanceof Map<?, ?> scene) {
                            blocks(novel, revision, scene.get("blocks"));
                            if (scene.get("blocks") instanceof List<?> list && list.isEmpty())
                                ledger.mark(novel, revision, (String) scene.get("id"));
                        }
                    }
                }
            }
        }
        blocks(novel, revision, value.get("blocks"));
        if (Integer.valueOf(0).equals(value.get("block_count")) && value.get("scene_id") instanceof String scene)
            ledger.mark(novel, revision, scene);
    }

    private void blocks(String novel, String revision, Object value) {
        if (!(value instanceof List<?> blocks)) return;
        for (Object raw : blocks) {
            if (raw instanceof Map<?, ?> block && block.get("text") instanceof String)
                ledger.mark(novel, revision, (String) (block.containsKey("block_id") ? block.get("block_id") : block.get("id")));
        }
    }
}
