package com.zyy.iam.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * View Object for menu data presentation (tree structure).
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysMenuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentId;

    private String menuName;

    private String menuType;

    private String path;

    private String icon;

    private String permission;

    private Integer sortOrder;

    private Integer status;

    private List<SysMenuVO> children;

    private LocalDateTime createTime;
}
