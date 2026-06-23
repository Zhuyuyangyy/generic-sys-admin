package com.zyy.asset.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * View Object for location data presentation.
 * <p>
 * Includes children list for tree structure rendering.
 *
 * @author System Architect
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String locationName;
    private String locationCode;
    private Long parentId;
    private String address;
    private String description;
    private Integer status;
    private String statusText;
    private LocalDateTime createTime;
    private List<LocationVO> children;
}
