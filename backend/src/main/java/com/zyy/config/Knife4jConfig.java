package com.zyy.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * ==========================================================
 * Knife4j + OpenAPI3 Configuration
 * Swagger API Documentation with Mock Response Examples
 * ==========================================================
 *
 * Features:
 * - Bearer token authentication (JWT)
 * - Mock response examples for Try It Out functionality
 * - Standard API response schemas
 *
 * @author System Architect
 */
@Configuration
public class Knife4jConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {

        Info info = new Info()
            .title("Generic System Administration Platform API")
            .description(buildDescription())
            .version("1.0.0")
            .contact(new Contact()
                .name("System Architect")
                .email("dev@besheep.cn")
                .url("https://github.com/besheep/generic-sys-admin"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT"));

        List<Server> servers = List.of(
            new Server()
                .url("http://localhost:8080")
                .description("Local Development Server"),
            new Server()
                .url("${APP_API_BASE_URL:https://api.dev.example.com}")
                .description("Production API Server")
        );

        Components components = new Components()
            .addSecuritySchemes(
                SECURITY_SCHEME_NAME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description(buildAuthDescription())
                    .extensions(null)
            )
            .addSchemas("Result", buildResultSchema())
            .addSchemas("PageResult", buildPageResultSchema());

        List<SecurityRequirement> securityRequirements = List.of(
            new SecurityRequirement().addList(SECURITY_SCHEME_NAME)
        );

        return new OpenAPI()
            .info(info)
            .servers(servers)
            .components(components)
            .security(securityRequirements);
    }

    @Bean
    public OpenApiCustomizer globalResponseExamplesCustomizer() {
        return openApi -> {
            var paths = openApi.getPaths();

            paths.forEach((path, pathItem) -> {
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    addSuccessResponseExample(operation);
                });
            });
        };
    }

    private void addSuccessResponseExample(io.swagger.v3.oas.models.Operation operation) {
        if (operation.getResponses() == null) return;

        ApiResponses responses = operation.getResponses();

        // Add 200 OK response
        if (!responses.containsKey("200")) {
            responses.addApiResponse("200", new ApiResponse()
                .description("Operation successful")
                .content(new Content().addMediaType(
                    "application/json",
                    new MediaType()
                        .example(buildSuccessExample(operation.getTags()))
                ))
            );
        }

        // Add 401 Unauthorized response
        if (!responses.containsKey("401")) {
            responses.addApiResponse("401", new ApiResponse()
                .description("Unauthorized - missing or invalid token")
                .content(new Content().addMediaType(
                    "application/json",
                    new MediaType().example("{\n  \"code\": 401,\n  \"message\": \"Invalid or expired token\",\n  \"data\": null\n}")
                ))
            );
        }

        // Add 403 Forbidden response
        if (!responses.containsKey("403")) {
            responses.addApiResponse("403", new ApiResponse()
                .description("Forbidden - insufficient permissions")
                .content(new Content().addMediaType(
                    "application/json",
                    new MediaType().example("{\n  \"code\": 403,\n  \"message\": \"Access denied\",\n  \"data\": null\n}")
                ))
            );
        }

        // Add 500 Internal Server Error response
        if (!responses.containsKey("500")) {
            responses.addApiResponse("500", new ApiResponse()
                .description("Internal server error")
                .content(new Content().addMediaType(
                    "application/json",
                    new MediaType().example("{\n  \"code\": 500,\n  \"message\": \"Internal server error\",\n  \"data\": null\n}")
                ))
            );
        }
    }

    private String buildSuccessExample(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return defaultSuccessExample();
        }

        String tag = tags.get(0);

        if (tag.contains("Upload") || tag.contains("File")) {
            return fileUploadSuccessExample();
        }
        if (tag.contains("TTS") || tag.contains("Voice")) {
            return ttsSuccessExample();
        }
        if (tag.contains("User")) {
            return userSuccessExample();
        }
        if (tag.contains("Role")) {
            return roleSuccessExample();
        }
        if (tag.contains("Login") || tag.contains("Auth")) {
            return loginSuccessExample();
        }
        if (tag.contains("Log")) {
            return logSuccessExample();
        }

        return defaultSuccessExample();
    }

    private String defaultSuccessExample() {
        return "{\n  \"code\": 200,\n  \"message\": \"Success\",\n  \"data\": {}\n}";
    }

    private String fileUploadSuccessExample() {
        return "{\n  \"code\": 200,\n  \"message\": \"Success\",\n  \"data\": {\n    \"url\": \"/mock/audio/f7c3a8b1-5d2e-4f9m-8n1o.jpg\",\n    \"filename\": \"f7c3a8b1-5d2e-4f9m-8n1o.jpg\",\n    \"originalName\": \"test-image.jpg\",\n    \"size\": 102400,\n    \"contentType\": \"image/jpeg\"\n  }\n}";
    }

    private String ttsSuccessExample() {
        return "{\n  \"code\": 200,\n  \"message\": \"Success\",\n  \"data\": \"/mock/audio/f7c3a8b1-5d2e-4f9m-8n1o.mp3\"\n}";
    }

    private String userSuccessExample() {
        return "{\n  \"code\": 200,\n  \"message\": \"Success\",\n  \"data\": {\n    \"id\": \"1876543210123456789\",\n    \"username\": \"admin\",\n    \"nickname\": \"Administrator\",\n    \"email\": \"admin@example.com\",\n    \"avatar\": \"/local-files/2026/04/default-avatar.png\",\n    \"roles\": [\"admin\"],\n    \"isDeleted\": 0,\n    \"createTime\": \"2026-04-19T10:00:00\"\n  }\n}";
    }

    private String roleSuccessExample() {
        return "{\n  \"code\": 200,\n  \"message\": \"Success\",\n  \"data\": {\n    \"id\": \"1876543210123456788\",\n    \"roleName\": \"admin\",\n    \"roleKey\": \"admin\",\n    \"description\": \"System Administrator\",\n    \"sort\": 1,\n    \"isDeleted\": 0,\n    \"createTime\": \"2026-04-19T10:00:00\"\n  }\n}";
    }

    private String loginSuccessExample() {
        return "{\n  \"code\": 200,\n  \"message\": \"Login successful\",\n  \"data\": {\n    \"token\": \"eyJhbGciOiJIUzI1NiJ9...\",\n    \"refreshToken\": \"eyJhbGciOiJIUzI1NiJ9...\",\n    \"userId\": \"1876543210123456789\",\n    \"username\": \"admin\",\n    \"expiresIn\": 7200\n  }\n}";
    }

    private String logSuccessExample() {
        return "{\n  \"code\": 200,\n  \"message\": \"Success\",\n  \"data\": {\n    \"records\": [\n      {\n        \"id\": \"1876543210123456787\",\n        \"username\": \"admin\",\n        \"operation\": \"Create new user\",\n        \"method\": \"UserController.delete\",\n        \"ip\": \"127.0.0.1\",\n        \"status\": 1,\n        \"createTime\": \"2026-04-19 10:30:00\"\n      }\n    ],\n    \"total\": 1,\n    \"current\": 1,\n    \"size\": 10\n  }\n}";
    }

    // ==================== Schema Definitions ====================

    private Schema<?> buildResultSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.setProperties(Map.of(
            "code", new Schema<>().type("integer").description("HTTP status code").example(200),
            "message", new Schema<>().type("string").description("Response message").example("Success"),
            "data", new Schema<>().type("object").description("Response data").nullable(true)
        ));
        schema.setRequired(List.of("code", "message"));
        return schema;
    }

    private Schema<?> buildPageResultSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.setProperties(Map.of(
            "records", new Schema<>().type("array").description("Data records"),
            "total", new Schema<>().type("long").description("Total count").example(100),
            "current", new Schema<>().type("long").description("Current page").example(1),
            "size", new Schema<>().type("long").description("Page size").example(10)
        ));
        return schema;
    }

    // ==================== Documentation ====================

    private String buildDescription() {
        return "## Generic System Administration Platform API\n\n" +
            "### Mock Mode\n" +
            "When API keys are not configured, Mock responses will be returned.\n\n" +
            "### Authentication\n" +
            "1. **Get Token**: POST /api/auth/login\n" +
            "2. **Authorize**: Add Authorization header with Bearer token\n\n" +
            "### Response Format\n" +
            "```json\n" +
            "{\n" +
            "  \"code\": 200,\n" +
            "  \"message\": \"Success\",\n" +
            "  \"data\": {}\n" +
            "}\n" +
            "```\n\n" +
            "### Error Codes\n" +
            "| Code | Description |\n" +
            "|------|-------------|\n" +
            "| 200  | Success |\n" +
            "| 400  | Bad Request |\n" +
            "| 401  | Unauthorized |\n" +
            "| 403  | Forbidden |\n" +
            "| 404  | Not Found |\n" +
            "| 500  | Internal Server Error |\n";
    }

    private String buildAuthDescription() {
        return "## JWT Token Authentication\n\n" +
            "**Step 1**: Login to get token\n" +
            "```\n" +
            "POST /api/auth/login\n" +
            "Content-Type: application/json\n\n" +
            "{\n" +
            "  \"username\": \"admin\",\n" +
            "  \"password\": \"123456\"\n" +
            "}\n" +
            "```\n\n" +
            "**Step 2**: Copy the `token` from response\n" +
            "**Step 3**: Click Authorize button\n" +
            "**Step 4**: Enter `Bearer <token>`\n";
    }
}
