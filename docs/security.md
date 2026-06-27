# ERMS Security Policy

## Authentication

### JWT-Based Stateless Authentication

The system uses JSON Web Tokens (JWT) for stateless authentication. No server-side sessions are stored.

**Token Configuration (from `application.yml`):**
- Access token expiration: `JWT_EXPIRATION` (default 7200 seconds = 2 hours)
- Refresh token expiration: `JWT_REFRESH_EXPIRATION` (default 604800 seconds = 7 days)
- Signing algorithm: HMAC-SHA256
- Signing key: `JWT_SECRET` (minimum 32 characters, **must be overridden in production**)

**Token Claims:**
```json
{
  "sub": "1",
  "username": "admin",
  "userId": 1,
  "permissions": ["system:user:query", "system:role:add", ...],
  "iat": 1718000000,
  "exp": 1718007200
}
```

**Authentication Flow:**
1. Client sends `POST /api/users/login` with username and password
2. Server verifies password using BCrypt comparison
3. On success, server returns `accessToken` and `refreshToken`
4. Client includes `Authorization: Bearer <accessToken>` on subsequent requests
5. `JwtAuthFilter` validates the token and populates `SecurityContext`
6. Expired tokens can be refreshed via `POST /api/users/refresh-token`

### Password Storage

- **Algorithm**: BCrypt (via `BCryptPasswordEncoder`)
- **Strength**: Default cost factor (10 rounds)
- **Storage**: Hashed password stored in `sys_user.password` column (VARCHAR 255)
- **Plain text**: Never stored or logged
- **Default passwords**: `admin/123456` and `user/123456` — **must be changed before production**

### Account Lockout

The system tracks failed login attempts:
- `sys_user.failed_attempts`: Incremented on each failed attempt
- `sys_user.locked_until`: Lockout expiration timestamp
- Account status (`status=2`) can be set to locked by administrators
- Lock/unlock endpoints: `POST /api/users/{id}/lock`, `POST /api/users/{id}/unlock`

## Authorization

### RBAC Model

```
User ──(sys_user_role)──→ Role ──(sys_role_menu)──→ Menu/Permission
```

**Three-level hierarchy:**
1. **Directory** (menu_type=1): Top-level navigation grouping
2. **Menu** (menu_type=2): Route-level page access
3. **Button** (menu_type=3): Fine-grained action permission

**Permission Format:** `module:entity:action`
- Examples: `system:user:query`, `system:role:add`, `system:role:edit`, `system:role:del`, `system:role:grant`
- Dashboard: `dashboard:view`
- Audit: `sys:log:list`, `sys:log:query`

### Pre-Configured Roles

| Role | Code | Scope |
|------|------|-------|
| Super Administrator | `SUPER_ADMIN` | All menus and permissions |
| Normal User | `USER` | Dashboard only |

### Authorization Enforcement

1. **URL-level**: `WebSecurityConfig` defines permit-all and authenticated patterns
2. **Method-level**: `@PreAuthorize` annotations on controller methods
   - `@PreAuthorize("@ss.hasAuthority('system:role:list')")`
   - `@PreAuthorize("hasAuthority('dashboard:view')")`
3. **Dynamic menus**: `GET /api/menus/current` returns only menus the user's roles permit

### Public Endpoints (No Authentication Required)

- `POST /api/users/login` — Login
- `POST /api/users` — User registration
- `/api/ai/**` — AI services (demo)
- `/swagger-ui/**`, `/v3/api-docs/**`, `/doc.html` — API documentation

## Audit Logging

### Automatic Operation Logging

All controller method invocations are intercepted by `OperationLogAspect`:

- **Mechanism**: AOP `@Around` advice on `execution(* com.zyy..*Controller.*(..))`
- **Data captured**:
  - Operator: userId, username (from SecurityContext)
  - Request: HTTP method, URL, client IP, User-Agent, parameters (JSON)
  - Execution: duration in ms, success/failure status
  - Error: exception message (truncated to 500 chars) on failure
- **Enhancement**: Methods annotated with `@Log` provide explicit module/operation metadata
- **Fallback**: When `@Log` is absent, module is inferred from class name and operation from method name prefix
- **Persistence**: Logs are saved to `sys_operation_log` table asynchronously
- **Real-time**: Operations are also pushed via WebSocket for live monitoring

### Log Integrity

- Logs are append-only (no update/delete API exposed)
- Each log entry includes a timestamp and the operator's identity
- Failed operations are recorded with error details

## Environment Variable Management

### Principle

No secrets are hardcoded in source code. All sensitive values are externalized through environment variables with safe defaults for development.

### Configuration in `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:generic_sys_admin}...
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
  redis:
    host: ${REDIS_HOST:localhost}
    password: ${REDIS_PASSWORD:}

jwt:
  secret: ${JWT_SECRET:please-change-this-secret-in-production-at-least-32-chars}
  expiration: ${JWT_EXPIRATION:7200}

minimax:
  api-key: ${MINIMAX_API_KEY:}
```

### `.env` File

- `.env.example` is committed to version control as a template
- `.env` is listed in `.gitignore` and must **never** be committed
- All secrets are injected at deployment time via Docker Compose environment variables

## CORS Configuration

Current configuration in `WebSecurityConfig`:

```java
config.setAllowedOriginPatterns(List.of("*"));
config.setAllowedMethods(List.of("*"));
config.setAllowedHeaders(List.of("*"));
config.setAllowCredentials(true);
config.setMaxAge(3600L);
```

**Production Recommendation:**
- Replace `allowedOriginPatterns("*")` with specific domain(s)
- Restrict `allowedMethods` to only needed HTTP methods
- This is a known hardening item for v0.6

## File Upload Restrictions

### Allowed File Types

| Category | Extensions |
|----------|-----------|
| Images | jpg, jpeg, png, gif, bmp, webp |
| Documents | pdf, doc, docx, xls, xlsx, ppt, pptx, txt |

### Size Limits

- Single file: 10 MB maximum (`spring.servlet.multipart.max-file-size=10MB`)
- Total request: 10 MB maximum (`spring.servlet.multipart.max-request-size=10MB`)
- Batch upload: Maximum 20 files per request

### Validation

- File extension is validated against the allowlist before storage
- File size is checked before upload proceeds
- `HttpServletRequest` and `HttpServletResponse` objects are excluded from parameter serialization in audit logs

### Storage Security

- MinIO: Object storage with configurable access keys
- Local fallback: Files stored in local filesystem when MinIO is unavailable
- No direct file path exposure in API responses

## Production Security Checklist

### Critical (Must Do Before Deployment)

- [ ] **Change JWT_SECRET** to a cryptographically random string (minimum 32 characters): `openssl rand -base64 48`
- [ ] **Change all default passwords**: DB, Redis, MinIO, admin user accounts
- [ ] **Restrict CORS** origins from `*` to specific domain(s)
- [ ] **Remove AI endpoint public access**: `/api/ai/**` should require authentication in production
- [ ] **Restrict port exposure**: Do not expose MySQL (3306) and Redis (6379) externally
- [ ] **Set `SPRING_PROFILES=prod`** to use production configuration
- [ ] **Enable Redis password**: Set a strong `REDIS_PASSWORD`

### Important (Should Do)

- [ ] **Set up TLS/HTTPS**: Configure SSL certificates in Nginx
- [ ] **Enable MySQL SSL**: Set `useSSL=true` in JDBC connection string
- [ ] **Review user registration**: Consider requiring authentication for `POST /api/users`
- [ ] **Implement rate limiting**: Add rate limits on login and registration endpoints
- [ ] **Set up CSP headers**: Configure Content-Security-Policy in Nginx
- [ ] **Audit log retention policy**: Define retention period and archival strategy
- [ ] **Review WebSocket security**: Ensure WebSocket connections are authenticated

### Recommended (Nice to Have)

- [ ] **IP whitelist** for admin endpoints
- [ ] **Two-factor authentication** for admin accounts
- [ ] **Security headers**: X-Frame-Options, X-Content-Type-Options, X-XSS-Protection
- [ ] **Request size limits** at the Nginx level
- [ ] **Intrusion detection**: Set up monitoring for suspicious login patterns
- [ ] **Penetration testing**: Conduct a security assessment before going live

## Known Security Limitations

1. **CORS is permissive**: Current `allowedOriginPatterns("*")` allows any origin (hardening planned for v0.6)
2. **AI endpoints are public**: `/api/ai/**` is permit-all for demo purposes
3. **User registration is public**: `POST /api/users` requires no authentication
4. **WebSocket has no auth**: WebSocket connections do not currently validate JWT tokens
5. **No rate limiting**: Login and registration endpoints have no rate protection
6. **In-memory confirmation store**: NL confirmation IDs are stored in memory and lost on restart
