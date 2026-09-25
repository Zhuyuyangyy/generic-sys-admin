# Write-Side-Effect Audit

Every operation in the codebase that changes state, and whether a repeated
request is dangerous.

Audited from source, not from documentation. "Naturally idempotent" means
repeating the request on an already-completed state produces no further state
change.

| Operation | Endpoint / entry point | Side effect | Duplicate dangerous? | Naturally idempotent? | Needs protection? |
|---|---|---|---|---|---|
| Login | `POST /api/users/login` | updates `last_login_ip` / `last_login_at`; issues tokens | No — overwrites, does not accumulate | **Yes** | No |
| Refresh token | `POST /api/users/refresh-token` | issues new tokens | No — new pair replaces old | **Yes** | No |
| Register user | `POST /api/users` | inserts `sys_user` row | **Yes** — duplicate usernames are rejected by a uniqueness check, but the same *valid* payload retried after a client timeout yields `Username already exists`, not a second row | **Yes** (unique key on `username`) | No |
| Update profile | `PUT /api/users/{id}`, `PUT /api/users/me` | overwrites columns | No | **Yes** | No |
| Change password | `PUT /api/users/me/password` | overwrites hash | No | **Yes** | No |
| Set status / lock / unlock | `PATCH /api/users/{id}/status`, `POST .../lock`, `POST .../unlock` | overwrites `status` | No | **Yes** | No |
| Delete user | `DELETE /api/users/{id}` | soft-delete (`is_deleted = 1`) | No — second delete is a no-op | **Yes** | No |
| Create / update / delete role | `POST/PUT/DELETE /api/roles` | inserts or soft-deletes `sys_role` | Delete yes; create/update no | Partly | No |
| **Grant menus to role** | `PUT /api/roles/{id}/menus` | **deletes and re-inserts all `sys_role_menu` rows** | **Yes** — a retry clears the assignment twice; with a changed payload it silently overwrites the first grant | No — it is replace-all, so a retry is a no-op but a *different* payload wins | **Yes** (see note) |
| Consumable create / update / delete | `POST/PUT/DELETE /api/consumables/{id}` | insert / overwrite / soft-delete | Delete yes | Partly | No |
| **Stock inbound** | `POST /api/consumables/{id}/inbound` | `stock_quantity += n` **and** inserts an `sys_inventory_transaction` row | **Yes** — a retry adds stock again and inserts a second ledger row | **No** | **Yes** — highest priority |
| **Stock outbound** | `POST /api/consumables/{id}/outbound` | `stock_quantity -= n` **and** inserts a ledger row | **Yes** — a retry deducts again | **No** | **Yes** — highest priority |
| Stock adjust | `PATCH /api/consumables/{id}/stock` | `stock_quantity += delta` + ledger row | **Yes** — same as inbound | **No** | **Yes** |
| Equipment create / update / delete / status | `/api/equipment/**` | insert / overwrite / soft-delete | Delete yes | Partly | No |
| Inspection create / update / delete | `/api/inventory-records/**` | insert / overwrite / soft-delete | Delete yes | Partly | No |
| File upload | `POST /api/files/upload`, `/upload/batch` | writes object to MinIO or local disk | **Yes** — a retry stores another copy and returns a new URL | **No** | **No** — duplicates are harmless, the caller holds the new URL |
| File delete | `DELETE /api/files`, `/batch` | removes object | No | **Yes** | No |
| AI TTS / image / video | `POST /api/ai/tts`, `/image`, `/video/generate` | external API call, returns artifact URL | **Yes** — a retry spends quota again | **No** | **No** — cost, not correctness; a key would need to be stored and matched against the result URL |
| **NL execute** | `POST /api/nl/execute`, `/execute-with-causal-check` | whatever the resolved command does — ultimately one of the write operations above | Inherits the risk of the underlying operation | **No** | **Inherited** — see note |

## Conclusions

### Must protect: inventory inbound / outbound / adjust

These three are the only operations where a duplicate request **corrupts an
invariant**: stock is a running total, so a retry changes the balance, and the
transaction ledger grows a phantom row. Everything else is either overwrite-only
(addressed by a natural key) or delete-only (naturally idempotent).

They also share one code path (`ConsumableServiceImpl.adjustStock`), so
protecting it protects all three.

### Deliberately not protected

- **AI calls** — a duplicate costs quota but corrupts nothing. Adding an
  idempotency key would mean persisting the key *and* the external artifact URL,
  which is a different and larger feature.
- **File upload** — the return value is the new URL; a duplicate is a second
  object, not a corrupted first one.
- **NL execute** — must **not** get its own mechanism. It resolves to one of the
  write operations above and should reuse whatever protection those get,
  otherwise REST and NL drift apart.

### Open risk: role grant is replace-all

`PUT /api/roles/{id}/menus` deletes and reinserts `sys_role_menu`. A retry with
the same payload is harmless; a retry with a *different* payload silently wins.
This is a privilege-escalation hazard rather than a duplicate-cost hazard, and
it is a semantics decision (last-write-wins vs. 409) that has not been made.

## Recommended mechanism

For inventory only: an `idempotency_key` column on `sys_inventory_transaction`
carrying a unique constraint on `(operator_id, consumable_id, idempotency_key)`
would collapse a retry at the ledger insert, which then also prevents the stock
change because both happen in one transaction. Same key + different payload →
409.

## Status: IMPLEMENTED (V1.3)

Implemented as described below, with one deliberate deviation from this draft:
`consumable_id` and `quantity` are NOT part of the unique key (they are payload
and belong in the request fingerprint). The key scope is
`(operator_id, operation, idempotency_key)`.

- Table: `sys_idempotency_record`
  (`V1.3__inventory_idempotency.sql`), columns `operator_id`, `operation`,
  `idempotency_key`, `request_hash`, `status` (PENDING/SUCCESS/FAILED),
  `result_reference`, `error_detail`, timestamps.
- Unique constraint: `uk_idempotency (operator_id, operation, idempotency_key)`.
- Claim is a plain INSERT with no existence check — the constraint is the
  arbiter, so it holds under concurrency and across instances.
- The claim, the atomic `stock = stock + delta`, and the ledger insert share one
  transaction; a rollback removes all three.
- Same key + same payload on a committed request replays the original outcome
  without a second mutation. Same key + different payload → 409. FAILED rows are
  deleted so the key can be retried; SUCCESS rows never are.
- Header is optional (`Idempotency-Key`); existing clients that send nothing keep
  the legacy unprotected behaviour.
- NL inherits the same enforcement because `NLExecutor` resolves to the same
  service methods — there is no second mechanism.

Covered by `ConsumableIdempotencyTest` (9) and the HTTP-layer cases in
`SecurityRuntimeAuthorizationTest` (5).
