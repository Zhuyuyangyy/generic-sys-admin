package com.zyy.iam.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新Token请求DTO
 */
@Data
@Schema(description = "刷新Token请求")
public class RefreshTokenDTO {

    @NotBlank(message = "refreshToken不能为空")
    @Schema(description = "刷新令牌")
    private String refreshToken;
}
