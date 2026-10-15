package com.mianjingge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ImportParseRequest {

    @NotBlank(message = "md 内容不能为空")
    @Size(max = 200_000, message = "md 内容过长")
    private String content;
}
