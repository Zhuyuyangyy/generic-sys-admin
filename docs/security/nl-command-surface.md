# NL Command Surface

Everything the natural-language path can trigger. This is the complete
authoritative list — `NLExecutor` refuses anything not in its whitelist.

## Pipeline

```
user text
   │
   ▼  HybridNLParser            rule engine; LLM fallback only if DEEPSEEK_API_KEY set
intent (QUERY|CREATE|UPDATE|DELETE|STATISTICS) + entityIds + entityType
   │
   ▼  NLRuleEngine.resolveService(intent, entityType)
a service-method name, or null when the (intent, entityType) pair is unknown
   │
   ▼  NLExecutor.describe(name)
Command descriptor — or "not whitelisted", and nothing is executed
   │
   ▼  EquipmentService / ConsumableService / InventoryRecordService, by exact method name
```

`NLController` additionally requires the `nl:execute` authority, so the whole
path is behind authentication + authorization + whitelist.

## Registered commands

| # | Service method | Target service | Action | Authority required | Needs entity ID |
|---|---|---|---|---|---|
| 1 | `getEquipmentById` | Equipment | READ | `equipment:list` | yes |
| 2 | `listEquipments` | Equipment | READ | `equipment:list` | no |
| 3 | `countEquipments` | Equipment | READ | `equipment:list` | no |
| 4 | `deleteEquipment` | Equipment | DELETE | `equipment:del` | yes |
| 5 | `updateEquipment` | Equipment | WRITE | `equipment:edit` | yes |
| 6 | `createEquipment` | Equipment | WRITE | `equipment:add` | yes |
| 7 | `getConsumableById` | Consumable | READ | `consumable:list` | yes |
| 8 | `countConsumables` | Consumable | READ | `consumable:list` | no |
| 9 | `deleteConsumable` | Consumable | DELETE | `consumable:del` | yes |
| 10 | `updateConsumable` | Consumable | WRITE | `consumable:edit` | yes |
| 11 | `createConsumable` | Consumable | WRITE | `consumable:add` | yes |
| 12 | `getInspectionById` | InventoryRecord | READ | `inventory:list` | yes |
| 13 | `countInspections` | InventoryRecord | READ | `inventory:list` | no |
| 14 | `deleteInspection` | InventoryRecord | DELETE | `inventory:list` | yes |
| 15 | `updateInspection` | InventoryRecord | WRITE | `inventory:list` | yes |
| 16 | `createInspection` | InventoryRecord | WRITE | `inventory:list` | yes |

Determined by `nl_service` × `NLRuleEngine`'s `SERVICE_MAP`: only
`EQUIPMENT`, `CONSUMABLE`, and `INSPECTION` entity types are recognised, and
`NLParser` derives the type from an ID prefix or the keywords 设备 / 耗材 / 巡检.

## What a user cannot do

- **Cannot reach an unregistered Java method.** `dispatch()` resolves by exact
  name against the table above. There is no `startsWith` matching, no
  `Class.forName`, and no method name constructed from input.
- **Cannot fall through to a made-up method.** `NLRuleEngine` used to return
  `intent + "Entity"` (e.g. `"DELETEEntity"`) as a fallback; it now returns
  `null` and the caller reports a readable message.
- **Cannot influence which service is called.** The `service` field of the
  descriptor is a constant, not parsed from the input.
- **Cannot expand the call surface with a crafted entity ID.** IDs are parsed as
  strictly `<PREFIX>-<digits>`; anything else is rejected before a service is
  touched.

## Entity ID validation

`parseEntityId()` accepts `EQ-<digits>`, `CS-<digits>`, `IN-<digits>` and
rejects — with a fixed error string that does not echo the input:

| Input | Result |
|---|---|
| `EQ-123` | accepted → `123` |
| `../../etc/passwd` | rejected (no `-`, non-digit) |
| `1 OR 1=1` | rejected (no `-`) |
| `EQ-1;DROP TABLE` | rejected (`;` is not a digit) |
| `EQ-abc` | rejected |
| `EQ-` | rejected (empty numeric part) |
| `EQ--1` | rejected |
| `EQ-1.5` | rejected |
| `EQ-99999999999999999999999` | rejected (out of `long` range) |
| `getClass`, `toString`, `wait`, `class`, `__proto__` | cannot appear as a method name; all rejected at the whitelist step |
| `""` / blank | treated as "no ID"; commands needing an ID are rejected |

## Authorization on the NL path

Each command carries the authority it needs, and `dispatch()` **enforces it**:

```java
String denied = checkAuthority(cmd);   // 未登录 or 缺少 cmd.permission() → 拒绝
if (denied != null) { return denied; }
```

So `nl:execute` alone is not enough: a caller without `equipment:del` cannot
delete equipment through natural language, even though they can reach
`POST /api/nl/execute`. An unauthorised attempt returns
`没有执行该操作的权限：<permission>` and is logged with the required permission
and the caller's username.

The check sits **after** entity-id validation, so a malformed id is still
reported as `无法识别的实体ID` and never makes it as far as authorization.

`NLExecutorTest` covers: unauthenticated rejection, `nl:execute`-only caller
refused on `deleteEquipment`, permitted caller reaching execution, read command
needing only its read authority, and permission/action pairing.

## Gaps worth knowing

1. **`createEquipment` / `createConsumable` / `createInspection` are
   unreachable.** `NLRuleEngine.SERVICE_MAP` maps CREATE to those names, but
   `dispatch()` requires an entity ID for them and the parser cannot produce one
   for a create intent. They are declared for completeness and currently fail
   with `无法识别的实体ID`.
2. **No idempotency.** Retrying `删除设备 EQ-1` twice issues two service calls.
3. **No audit of the NL path.** `OperationLogAspect` records controller
   invocations, so `POST /api/nl/execute` is logged, but the parsed intent,
   resolved command, and target ID are not captured.

## Regression tests

`backend/src/test/java/com/zyy/nl/NLExecutorTest.java` covers whitelist
enforcement, malformed IDs, action classification, and authority declaration on
every write/delete command.
