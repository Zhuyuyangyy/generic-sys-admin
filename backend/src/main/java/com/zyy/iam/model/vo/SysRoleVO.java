package com.zyy.iam.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * View Object for role data presentation.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysRoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer status;

    private String statusText;

    private LocalDateTime createTime;
}
