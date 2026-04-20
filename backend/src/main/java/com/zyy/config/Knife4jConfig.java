package com.zyy.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j（Swagger3/OpenAPI3）API文档配置
 *
 * 访问地址：
 * - Swagger UI:  /swagger-ui.html
 * - Knife4j UI: /doc.html（推荐，界面更美观）
 * - OpenAPI:    /v3/api-docs
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("通用管理系统 API")
                .description("## 接口说明\n\n" +
                    "1. **认证方式**：登录后获取 `accessToken`，在请求头中加入：\n" +
                    "   ```\n" +
                    "   Authorization: Bearer <accessToken>\n" +
                    "   ```\n" +
                    "2. **统一响应**：`{ code, message, data }`\n" +
                    "   - `code=200` 请求成功\n" +
                    "   - `code=401` 未登录\n" +
                    "   - `code=403` 无权限\n" +
                    "   - `code=500` 服务器错误\n")
                .contact(new Contact().name("系统管理员").url("admin@example.com"))
                .version("1.0.0"))
            .addSecurityItem(new SecurityRequirement().addList("Authorization"))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("Authorization",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("登录后获得的访问令牌")));
    }
}
