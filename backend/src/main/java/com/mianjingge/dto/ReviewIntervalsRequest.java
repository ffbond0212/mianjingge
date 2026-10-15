package com.mianjingge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReviewIntervalsRequest {

    /** 遗忘曲线间隔（天），2-10 档递增 */
    @NotNull(message = "间隔配置不能为空")
    @Size(min = 2, max = 10, message = "间隔需 2-10 档")
    private List<@Min(value = 1, message = "间隔 1-365 天") @Max(value = 365, message = "间隔 1-365 天") Integer> intervals;
}
