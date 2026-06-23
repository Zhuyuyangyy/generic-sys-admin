package com.zyy.audit.service;

import com.zyy.common.PageVO;
import com.zyy.audit.model.vo.OperationLogVO;

import java.time.LocalDateTime;

/**
 * Operation log business service interface.
 */
public interface OperationLogService {

    PageVO<OperationLogVO> getPage(Long pageNum, Long pageSize, String module,
                                   String operator, LocalDateTime startTime, LocalDateTime endTime);

    OperationLogVO getById(Long id);

    void delete(Long id);
}
