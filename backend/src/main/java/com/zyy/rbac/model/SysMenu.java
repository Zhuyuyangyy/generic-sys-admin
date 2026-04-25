package com.zyy.rbac.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统菜单实体
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer visible; // 0=隐藏，1=显示
    private Integer isExternal; // 0=否，1=是
    private String permission;
    private Integer menuType; // 1=目录，2=菜单，3=按钮
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
