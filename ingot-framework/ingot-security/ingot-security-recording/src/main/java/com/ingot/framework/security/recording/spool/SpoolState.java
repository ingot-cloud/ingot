package com.ingot.framework.security.recording.spool;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import lombok.Data;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <p>spool 持久化状态：未 claim、in-flight 与已 ack 的 segment 位置。</p>
 *
 * <p>落盘只包含条目元数据，事件 payload 留在 segment；写入走 tmp 再替换，避免截断 JSON 污染正式文件。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public final class SpoolState {

    /** 当前正在 append 的 segment 文件名；无则下次 enqueue 新建。 */
    private String activeSegment;
    /** 目录内 segment 占用字节，用于配额。 */
    private long totalBytes;
    /** 尚未 claim，或 nack / 启动回收后待重投的条目。 */
    private List<SpoolEntry> pending = new ArrayList<>();
    /** 已 claim、尚未 ack/nack 的条目。 */
    private List<SpoolEntry> inFlight = new ArrayList<>();

    static LoadResult load(Path stateFile, ObjectMapper objectMapper) {
        if (!Files.exists(stateFile)) {
            return LoadResult.missing();
        }
        try {
            SpoolState state = objectMapper.readValue(Files.readString(stateFile), SpoolState.class);
            if (state == null) {
                return LoadResult.corrupt();
            }
            if (state.getPending() == null) {
                state.setPending(new ArrayList<>());
            }
            if (state.getInFlight() == null) {
                state.setInFlight(new ArrayList<>());
            }
            return LoadResult.loaded(state);
        } catch (IOException e) {
            return LoadResult.corrupt();
        }
    }

    void save(Path stateFile, ObjectMapper objectMapper) throws IOException {
        Files.createDirectories(stateFile.getParent());
        Path tmp = stateFile.resolveSibling(stateFile.getFileName() + ".tmp");
        objectMapper.writeValue(tmp.toFile(), this);
        try {
            Files.move(tmp, stateFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, stateFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * <p>{@link SpoolState#load(Path, ObjectMapper)} 的三种结果：缺失、完好、损坏。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    static final class LoadResult {
        private final SpoolState state;
        private final boolean missing;
        private final boolean corrupt;

        private LoadResult(SpoolState state, boolean missing, boolean corrupt) {
            this.state = state;
            this.missing = missing;
            this.corrupt = corrupt;
        }

        static LoadResult missing() {
            return new LoadResult(new SpoolState(), true, false);
        }

        static LoadResult loaded(SpoolState state) {
            return new LoadResult(state, false, false);
        }

        static LoadResult corrupt() {
            return new LoadResult(new SpoolState(), false, true);
        }

        SpoolState state() {
            return state;
        }

        boolean isMissing() {
            return missing;
        }

        boolean isCorrupt() {
            return corrupt;
        }

        boolean needsSegmentRebuild() {
            return missing || corrupt;
        }
    }

    /**
     * <p>spool 内单条记录位置与 claim 元数据。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Data
    public static final class SpoolEntry {
        /** 本次 claim 标识，ack/nack 用。 */
        private String claimId;
        /** 所在 segment 文件名。 */
        private String segment;
        /** 记录在 segment 内的起始偏移。 */
        private long offset;
        /** 含帧头的字节长度。 */
        private int length;
        /** 已投递失败次数，用于退避。 */
        private int attempts;
        /** 下次允许 claim 的 epoch 毫秒；0 表示立即可领。 */
        private long nextRetryAtEpochMs;
        /** 内存中的事件；不写入 {@code state.json}，load 后从 segment hydrate。 */
        @JsonIgnore
        private SecurityEventRecord record;

        public SpoolEntry() {
        }

        SpoolEntry(String segment, long offset, int length, SecurityEventRecord record) {
            this.claimId = UUID.randomUUID().toString();
            this.segment = segment;
            this.offset = offset;
            this.length = length;
            this.record = record;
        }
    }
}
