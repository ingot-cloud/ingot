package com.ingot.component.id.snowflake.worker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : AbsWorkerIdFactory.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/25.</p>
 * <p>Time         : 3:07 下午.</p>
 */
@Slf4j
public abstract class AbsWorkerIdFactory implements WorkerIdFactory {
    private final String localCachePath;

    public AbsWorkerIdFactory(String prefix, String serviceName, String port) {
        if (!StrUtil.endWith(prefix, "/")) {
            prefix = prefix + "/";
        }
        this.localCachePath = prefix + serviceName + "/" + port + "/workerID.properties";
    }

    /**
     * 获取缓存的 work id
     *
     * @return 本地文件存储的 work id
     * @throws Exception 获取失败
     */
    protected int getCacheWorkId() throws Exception {
        Properties properties = new Properties();
        properties.load(Files.newInputStream(new File(localCachePath).toPath()));
        return Integer.parseInt(properties.getProperty("workerID"));
    }

    /**
     * 在节点文件系统上缓存一个 work id 值
     *
     * @param workerID worker id
     */
    protected void updateLocalWorkerID(int workerID) {
        File confFile = new File(localCachePath);
        boolean exists = confFile.exists();
        log.info("[AbsWorkerIdFactory] - file exists status is {}", exists);
        if (exists) {
            try {
                FileUtil.writeUtf8String("workerID=" + workerID, confFile);
                log.info("[AbsWorkerIdFactory] - update file cache workerID is {}", workerID);
            } catch (IORuntimeException e) {
                log.error("[AbsWorkerIdFactory] - update file cache error ", e);
            }
        } else {
            //不存在文件,父目录页肯定不存在
            try {
                boolean mkdirs = confFile.getParentFile().mkdirs();
                log.info("[AbsWorkerIdFactory] - init local file cache create parent dis status is {}, worker id is {}", mkdirs, workerID);
                if (mkdirs) {
                    if (confFile.createNewFile()) {
                        FileUtil.writeUtf8String("workerID=" + workerID, confFile);
                        log.info("[AbsWorkerIdFactory] - local file cache workerID is {}", workerID);
                    }
                } else {
                    log.warn("[AbsWorkerIdFactory] - create parent dir error");
                }
            } catch (IOException e) {
                log.warn("[AbsWorkerIdFactory] - create workerID conf file error", e);
            }
        }
    }
}
