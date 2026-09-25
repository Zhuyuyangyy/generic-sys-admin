# Permission Matrix

Extracted from the source. Every row is an actual endpoint in
`backend/src/main/java/com/zyy/controller/`.

Two enforcement layers:

- **URL layer** (`WebSecurityConfig`) — public: `POST /api/users/login`,
  `POST /api/users/refresh-token`, Swagger/Knife4j paths,
  `/actuator/health`, `/actuator/info`. Everything else requires authentication.
- **Method layer** — `@PreAuthorize` per endpoint, resolved by
  `SecurityChecker` (`@ss.hasAuthority`) against the authorities in the JWT.

`@ss.hasAuthority` is used instead of bare `hasAuthority(...)` because
`SecurityChecker` reads the `SecurityContext` directly, avoiding Spring
Security 6's SpEL principal-field restrictions.

## Endpoints

| Endpoint | HTTP | Authentication | Required permission | Write? |
|---|---|---|---|---|
| `/api/users/login` | POST | public | — | no |
| `/api/users/refresh-token` | POST | public | — | no |
| `/api/users` | POST | authenticated | `system:user:add` | **yes** |
| `/api/users/me` | GET | authenticated | — (self only) | no |
| `/api/users/me` | PUT | authenticated | — (self only) | **yes** |
| `/api/users/me/password` | PUT | authenticated | — (self only) | **yes** |
| `/api/users/{id}` | GET | authenticated | `system:user:list` | no |
| `/api/users` | GET | authenticated | `system:user:list` | no |
| `/api/users/{id}` | PUT | authenticated | `system:user:edit` | **yes** |
| `/api/users/{id}/status` | PATCH | authenticated | `system:user:edit` | **yes** |
| `/api/users/{id}/lock` | POST | authenticated | `system:user:edit` | **yes** |
| `/api/users/{id}/unlock` | POST | authenticated | `system:user:edit` | **yes** |
| `/api/users/{id}` | DELETE | authenticated | `system:user:del` | **yes** |
| `/api/roles` | GET | authenticated | `system:role:list` | no |
| `/api/roles/{id}` | GET | authenticated | `system:role:list` | no |
| `/api/roles/enabled` | GET | authenticated | — | no |
| `/api/roles` | POST | authenticated | `system:role:add` | **yes** |
| `/api/roles/{id}` | PUT | authenticated | `system:role:edit` | **yes** |
| `/api/roles/{id}` | DELETE | authenticated | `system:role:del` | **yes** |
| `/api/roles/{id}/menus` | PUT | authenticated | `system:role:grant` | **yes** |
| `/api/menus/current` | GET | authenticated | — (self only) | no |
| `/api/menus/tree` | GET | authenticated | `system:menu:list` | no |
| `/api/equipment` | POST | authenticated | `equipment:add` | **yes** |
| `/api/equipment/{id}` | PUT | authenticated | `equipment:edit` | **yes** |
| `/api/equipment/{id}` | GET | authenticated | `equipment:detail` | no |
| `/api/equipment` | GET | authenticated | `equipment:list` | no |
| `/api/equipment/{id}/status` | PATCH | authenticated | `equipment:edit` | **yes** |
| `/api/equipment/{id}` | DELETE | authenticated | `equipment:del` | **yes** |
| `/api/consumables` | POST | authenticated | `consumable:add` | **yes** |
| `/api/consumables/{id}` | PUT | authenticated | `consumable:edit` | **yes** |
| `/api/consumables/{id}` | GET | authenticated | `consumable:detail` | no |
| `/api/consumables` | GET | authenticated | `consumable:list` | no |
| `/api/consumables/{id}/inbound` | POST | authenticated | `consumable:in` | **yes** |
| `/api/consumables/{id}/outbound` | POST | authenticated | `consumable:out` | **yes** |
| `/api/consumables/{id}/stock` | PATCH | authenticated | `consumable:edit` | **yes** |
| `/api/consumables/{id}` | DELETE | authenticated | `consumable:del` | **yes** |
| `/api/inventory-records` | POST | authenticated | `inventory:add` | **yes** |
| `/api/inventory-records/{id}` | PUT | authenticated | `inventory:edit` | **yes** |
| `/api/inventory-records/{id}` | GET | authenticated | `inventory:list` | no |
| `/api/inventory-records` | GET | authenticated | `inventory:list` | no |
| `/api/inventory-records/equipment/{equipmentId}` | GET | authenticated | `inventory:list` | no |
| `/api/inventory-records/{id}` | DELETE | authenticated | `inventory:del` | **yes** |
| `/api/files/upload` | POST | authenticated | `file:upload` | **yes** |
| `/api/files/upload/batch` | POST | authenticated | `file:upload` | **yes** |
| `/api/files` | DELETE | authenticated | `file:del` | **yes** |
| `/api/files/batch` | DELETE | authenticated | `file:del` | **yes** |
| `/api/ai/tts` | POST | authenticated | `ai:tts` | **yes** |
| `/api/ai/image` | POST | authenticated | `ai:image` | **yes** |
| `/api/ai/video/generate` | POST | authenticated | `ai:video` | **yes** |
| `/api/ai/video/status/{jobId}` | GET | authenticated | `ai:video` | no |
| `/api/ai/models` | GET | authenticated | — | no |
| `/api/nl/execute` | POST | authenticated | `nl:execute` | **yes** |
| `/api/nl/execute-with-causal-check` | POST | authenticated | `nl:execute` | **yes** |
| `/api/nl/causal/dag` | GET | authenticated | — | no |
| `/api/dashboard/stats` | GET | authenticated | `dashboard:view` | no |
| `/api/logs` | GET | authenticated | `system:log:list` | no |
| `/api/logs/{id}` | GET | authenticated | `system:log:list` | no |
| `/actuator/**` | GET | authenticated | `ROLE_ADMIN` | no |
| `/swagger-ui/**`, `/v3/api-docs/**`, `/doc.html` | GET | public | — | no |

`MenuController` exposes read endpoints only (`GET /api/menus/current`,
`GET /api/menus/tree`) — there are no `/api/menus` POST/PUT/DELETE endpoints in
the codebase, so the `system:menu:add/edit/del` constants in `PermConst` and
their V1.2 seed rows are currently unused.

## Audit findings

### Fixed this round

| Finding | Evidence | Fix |
|---|---|---|
| `AIController`, `FileController`, `NLController` had **zero** `@PreAuthorize` | all write endpoints fell through to `anyRequest().authenticated()` | added per-endpoint authorities (`ai:tts`, `ai:image`, `ai:video`, `file:upload`, `file:del`, `nl:execute`) |
| `OperationLogController` required `sys:log:list` / `sys:log:query` | seed and `PermConst` define `system:log:list` — admin would have been 403 | unified on `system:log:list` |
| `DashboardController` used bare `hasAuthority(...)` | rest of the codebase uses `@ss.hasAuthority(...)` | unified for consistency |

### Remaining gaps

1. **`inventory-records` write operations were under-specified.**
   `POST /api/inventory-records` required `inventory:list` — a list permission
   gating a create, which is misleading — and `PUT`/`DELETE` required
   `ROLE_ADMIN`, bypassing the fine-grained permission model entirely.
   **Fixed:** `inventory:add` / `inventory:edit` / `inventory:del` added to
   `PermConst`, seeded in V1.2, and enforced per method.

2. **`MenuController` has no write endpoints.** It declares a read-only
   `@RequestMapping` — the `system:menu:add/edit/del` constants in `PermConst` and
   the corresponding V1.2 seed rows exist but nothing calls them. Either the
   write endpoints were never implemented, or they were removed. Not a
   vulnerability; recorded so the orphaned constants are not mistaken for
   coverage.

3. **No authorization is tenant-scoped.** `system:user:list` returns every
   user; `equipment:list` every asset. There is no multi-tenancy in this
   codebase, so this is a scoping decision rather than a defect, but it limits
   what "RBAC" means here.
