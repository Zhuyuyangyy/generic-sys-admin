package com.zyy.service;

import com.zyy.model.entity.SysOperationLogEntity;
import com.zyy.model.vo.PageVO;

/**
 * 操作日志 Service 接口
 */
public interface SysOperationLogService {

    /**
     * 异步保存操作日志
     */
    void saveLog(SysOperationLogEntity log);

    /**
     * 分页查询操作日志
     */
    PageVO<SysOperationLogEntity> getPage(Long pageNum, Long pageSize, String module, String operation, String operator, String status, String startTime, String endTime);
}