package com.mianjingge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewCheckinRequest {

    /** 1 答上 2 部分答上 3 未答上 */
    @NotNull(message = "请选择是否答上")
    @Min(value = 1, message = "是否答上取值 1-3")
    @Max(value = 3, message = "是否答上取值 1-3")
    private Integer answered;

    /** 0-10 分，可不填 */
    @Min(value = 0, message = "得分范围 0-10")
    @Max(value = 10, message = "得分范围 0-10")
    private Integer score;
}
