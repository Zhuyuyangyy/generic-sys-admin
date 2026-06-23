package com.zyy.asset.service;

import com.zyy.asset.model.vo.AssetHealthOverview;
import com.zyy.asset.model.vo.AssetHealthScore;

import java.util.List;

/**
 * Asset health scoring business service interface.
 */
public interface AssetHealthService {

    /**
     * Calculate health score for a single equipment.
     *
     * @param equipmentId equipment ID
     * @return health score details
     */
    AssetHealthScore calculateHealthScore(Long equipmentId);

    /**
     * Calculate health scores for all active equipment.
     *
     * @return list of health scores
     */
    List<AssetHealthScore> getAllHealthScores();

    /**
     * Get an overview summary of all equipment health.
     *
     * @return health overview
     */
    AssetHealthOverview getHealthOverview();
}
