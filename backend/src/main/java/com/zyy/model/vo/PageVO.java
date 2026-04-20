package com.zyy.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * View Object for paginated data responses.
 * <p>
 * Wraps a list of items with pagination metadata following the
 * Spring Data common convention for pageable responses.
 *
 * @author System Architect
 * @version 1.0.0
 * @param <T> Type of items in the page
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageVO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Page content items */
    private List<T> items;

    /** Current page number (0-indexed) */
    private Long pageNum;

    /** Page size (items per page) */
    private Long pageSize;

    /** Total number of items */
    private Long total;

    /** Total number of pages */
    private Long pages;

    /** Whether this is the first page */
    private Boolean isFirst;

    /** Whether this is the last page */
    private Boolean isLast;

    /** Whether there are pages after this one */
    private Boolean hasNext;

    /** Whether there are pages before this one */
    private Boolean hasPrevious;
}
