package com.litegateway.admin.common.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应体
 */
@Data
@Schema(description = "分页响应体")
public class PageBody<T> {

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "总页数")
    private Integer pages;

    @Schema(description = "每页大小")
    private Integer pageSize;

    @Schema(description = "当前页码")
    private Integer pageNum;

    @Schema(description = "数据列表")
    private List<T> list;

    public PageBody() {
    }

    public PageBody(List<T> list) {
        this.list = list;
        this.total = (long) (list == null ? 0 : list.size());
        this.pages = 1;
        this.pageSize = this.total.intValue();
        this.pageNum = 1;
    }

    public PageBody(Long total, Integer pages, Integer pageSize, Integer pageNum, List<T> list) {
        this.total = total;
        this.pages = pages;
        this.pageSize = pageSize;
        this.pageNum = pageNum;
        this.list = list;
    }

    /**
     * 转换分页数据
     */
    public <R> PageBody<R> map(Function<? super T, ? extends R> converter) {
        PageBody<R> result = new PageBody<>();
        result.setTotal(this.total);
        result.setPages(this.pages);
        result.setPageSize(this.pageSize);
        result.setPageNum(this.pageNum);
        if (this.list != null) {
            result.setList(this.list.stream().map(converter).collect(Collectors.toList()));
        }
        return result;
    }

    /**
     * 创建空分页
     */
    public static <T> PageBody<T> empty() {
        return new PageBody<>(0L, 0, 20, 1, null);
    }
}
