package com.zyy.dashboard;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Dashboard 统计聚合 VO
 */
@Data
@Builder
public class DashboardStatsVO {

    /** 设备统计 */
    private EquipmentStats equipment;

    /** 耗材统计 */
    private ConsumableStats consumable;

    /** 用户统计 */
    private UserStats user;

    /** 近7日操作日志趋势 */
    private List<LogTrend> logTrend;

    /** 设备状态分布 */
    private List<StatusCount> equipmentStatus;

    /** 操作类型分布 */
    private List<StatusCount> operationType;

    // ========== 子类型 ==========

    @Data
    @Builder
    public static class EquipmentStats {
        private Long total;
        private Long normal;
        private Long maintenance;
        private Long scrapped;
    }

    @Data
    @Builder
    public static class ConsumableStats {
        private Long total;
        private Long lowStock;
        private Long expiring;
    }

    @Data
    @Builder
    public static class UserStats {
        private Long total;
        private Long active;
        private Long disabled;
    }

    @Data
    @Builder
    public static class LogTrend {
        private String date;
        private Long count;
    }

    @Data
    @Builder
    public static class StatusCount {
        private String label;
        private Integer value;
    }
}