package com.zyy.service;

import com.zyy.model.vo.DashboardStatsVO;

/**
 * Dashboard 统计聚合 Service
 */
public interface DashboardService {

    /**
     * 获取首页聚合统计数据
     */
    DashboardStatsVO getStats();
}