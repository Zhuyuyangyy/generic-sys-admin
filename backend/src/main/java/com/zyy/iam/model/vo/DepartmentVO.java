package com.zyy.iam.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * View Object for department data presentation.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepartmentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deptName;
    private Long parentId;
    private Long leaderId;
    private String leaderName;
    private Integer sort;
    private Integer status;
    private String statusText;
    private LocalDateTime createTime;
    private List<DepartmentVO> children;
}
