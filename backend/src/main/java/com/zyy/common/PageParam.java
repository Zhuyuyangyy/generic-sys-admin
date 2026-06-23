package com.zyy.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页请求参数
 */
@Data
@Schema(description = "分页请求参数")
public class PageParam {

    @Schema(description = "当前页码", example = "1")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;

    @Schema(description = "排序字段")
    private String order = "createTime";

    @Schema(description = "排序方式：asc/desc")
    private String sort = "desc";

    // 兼容 MyBatis-Plus 的分页参数方法
    public Long getPageNum() {
        return current;
    }

    public void setPageNum(Long pageNum) {
        this.current = pageNum;
    }

    public Long getPageSize() {
        return size;
    }

    public void setPageSize(Long pageSize) {
        this.size = pageSize;
    }
}
