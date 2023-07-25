package com.ingot.framework.id.leaf;

import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import com.ingot.framework.id.BizGenerator;
import com.ingot.framework.id.leaf.common.Result;
import com.ingot.framework.id.leaf.common.Status;
import com.ingot.framework.id.leaf.segment.SegmentIDGenImpl;
import com.ingot.framework.id.leaf.segment.dao.IDAllocDao;
import com.ingot.framework.id.leaf.segment.dao.impl.IDAllocDaoImpl;
import com.ingot.framework.core.error.exception.BizException;
import com.ingot.framework.core.model.status.BaseErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : BizGeneratorImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/2/23.</p>
 * <p>Time         : 3:33 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultBizGenerator implements BizGenerator {
    private final DataSource dataSource;
    private IDGen idGen;

    @PostConstruct
    public void init() throws SQLException, InitException {
        // Config Dao
        IDAllocDao dao = new IDAllocDaoImpl(dataSource);

        // Config ID Gen
        idGen = new SegmentIDGenImpl();
        ((SegmentIDGenImpl) idGen).setDao(dao);
        if (idGen.init()) {
            log.info("BizGenerator - Segment Service Init Successfully");
        } else {
            throw new InitException("BizGenerator Segment Service Init Fail");
        }
    }

    @Override
    public long getId(String key) {
        Result result = idGen.get(key);
        if (result.getStatus() == Status.EXCEPTION) {
            throw new BizException(BaseErrorCode.INTERNAL_SERVER_ERROR,
                    "获取ID失败，错误码" + result.getId());
        }
        return result.getId();
    }
}
