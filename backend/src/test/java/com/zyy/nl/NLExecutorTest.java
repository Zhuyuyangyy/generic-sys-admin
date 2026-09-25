package com.zyy.nl;

import com.zyy.service.ConsumableService;
import com.zyy.service.EquipmentService;
import com.zyy.service.InventoryRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * NLExecutor 白名单、授权顺序与副作用测试。
 *
 * 守着四条底线：
 * 1. 方法名必须来自白名单，不能把任意字符串喂给反射层；
 * 2. 白名单中的 method 必须与 Service 接口真实方法名一致，否则反射必然失败；
 * 3. 授权顺序：白名单 → 认证 → 授权 → 参数校验 → service；
 * 4. 被拒绝的请求必须【没有】调用任何 business service。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NLExecutor 白名单、授权顺序与副作用测试")
class NLExecutorTest {

    private final NLExecutor executor = new NLExecutor();

    @Mock
    private EquipmentService equipmentService;

    @Mock
    private ConsumableService consumableService;

    @Mock
    private InventoryRecordService inspectionService;

    @BeforeEach
    void injectMocksAndClearContext() {
        ReflectionTestUtils.setField(executor, "equipmentService", equipmentService);
        ReflectionTestUtils.setField(executor, "consumableService", consumableService);
        ReflectionTestUtils.setField(executor, "inspectionService", inspectionService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void resetContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "tester", null,
                        List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()
                )
        );
    }

    private Class<?> serviceTypeFor(String svc) {
        return switch (svc) {
            case "equipment" -> EquipmentService.class;
            case "consumable" -> ConsumableService.class;
            default -> InventoryRecordService.class;
        };
    }

    // ==================== 白名单与 Service 签名一致性 ====================

    @Test
    @DisplayName("白名单中的 method 必须真实存在于对应 Service 接口")
    void whitelistedMethodsMustExistOnService() {
        String[][] candidates = {
                {"equipment", "getById"}, {"equipment", "getPage"}, {"equipment", "delete"},
                {"consumable", "getById"}, {"consumable", "getPage"}, {"consumable", "delete"},
                {"inspection", "getById"}, {"inspection", "delete"},
        };
        int checked = 0;
        for (String[] c : candidates) {
            if (!executor.isSupported(c[0], c[1])) {
                continue;
            }
            Class<?> serviceType = serviceTypeFor(c[0]);
            boolean exists = Arrays.stream(serviceType.getMethods())
                    .anyMatch(m -> m.getName().equals(c[1]));
            assertTrue(exists,
                    "白名单登记了 " + c[0] + "|" + c[1] + " 但 " + serviceType.getSimpleName()
                            + " 没有这个方法——反射调用必然失败并被吞成\"执行失败\"");
            checked++;
        }
        assertTrue(checked > 0, "没有检查到任何白名单命令");
    }

    @Test
    @DisplayName("白名单不包含任何拼造出来的方法名")
    void whitelistHasNoInventedMethodNames() {
        for (String bogus : new String[]{
                "getEquipmentById", "listEquipments", "countEquipments",
                "deleteEquipment", "updateEquipment", "createEquipment",
                "getConsumableById", "countConsumables", "deleteConsumable",
                "getInspectionById", "countInspections", "deleteInspection"
        }) {
            assertFalse(executor.isSupported("equipment", bogus), bogus);
            assertFalse(executor.isSupported("consumable", bogus), bogus);
            assertFalse(executor.isSupported("inspection", bogus), bogus);
        }
    }

    @Test
    @DisplayName("非白名单方法一律拒绝且不触达 service")
    void rejectsUnknownCommands() {
        authenticate("equipment:del", "nl:execute");
        for (String bogus : new String[]{
                "DELETEEntity", "deleteUser", "deleteEquipmentById", "getClass", "toString"
        }) {
            Object result = executor.dispatch("equipment", bogus, "EQ-1");
            assertEquals("该操作暂不支持通过自然语言执行", result, "必须拒绝: " + bogus);
        }
        verifyNoInteractions(equipmentService);
    }

    // ==================== 授权顺序（Gate A） ====================

    @Test
    @DisplayName("未认证时畸形 ID 也只报未登录，不进入参数解析（fail-closed）")
    void malformedIdDoesNotLeakWhenUnauthenticated() {
        Object result = executor.dispatch("equipment", "delete", "EQ-abc");
        assertEquals("未登录，无法执行该操作", result);
        verifyNoInteractions(equipmentService);
    }

    @Test
    @DisplayName("无权限时畸形 ID 也只报无权限，不进入参数解析（fail-closed）")
    void malformedIdDoesNotLeakWhenForbidden() {
        authenticate("nl:execute");   // 有 nl:execute，没有 equipment:del
        Object result = executor.dispatch("equipment", "delete", "EQ-abc");
        assertEquals("没有执行该操作的权限：equipment:del", result);
        verifyNoInteractions(equipmentService);
    }

    @Test
    @DisplayName("未认证时任何命令都不触达任何 service")
    void unauthenticatedNeverTouchesAnyService() {
        executor.dispatch("equipment", "delete", "EQ-1");
        executor.dispatch("equipment", "getById", "EQ-1");
        executor.dispatch("consumable", "delete", "CS-1");
        verifyNoInteractions(equipmentService, consumableService, inspectionService);
    }

    @Test
    @DisplayName("持有 nl:execute 但没有目标权限时，删除命令仍被拒绝且无副作用")
    void nlExecuteAloneIsNotEnoughForDelete() {
        authenticate("nl:execute");
        Object result = executor.dispatch("equipment", "delete", "EQ-1");
        assertEquals("没有执行该操作的权限：equipment:del", result);
        verifyNoInteractions(equipmentService);
    }

    @Test
    @DisplayName("无权限时 inventory 删除命令也不会触达 service")
    void forbiddenInspectionDeleteHasNoSideEffect() {
        authenticate("nl:execute");
        Object result = executor.dispatch("inspection", "delete", "IN-9");
        assertEquals("没有执行该操作的权限：inventory:del", result);
        verifyNoInteractions(inspectionService);
    }

    @Test
    @DisplayName("持有目标权限时命令放行（service 不需要第二个参数时能真正执行）")
    void allowedWhenAuthorityPresent() {
        authenticate("equipment:del");
        Object result = executor.dispatch("equipment", "delete", "EQ-7");
        // 已经通过白名单 → 认证 → 授权 → ID 解析四关，一定不是前三种拒绝
        assertNotEquals("没有执行该操作的权限：equipment:del", result);
        assertNotEquals("未登录，无法执行该操作", result);
        assertNotEquals("无法识别的实体ID", result);
        assertNotEquals("该操作暂不支持通过自然语言执行", result);
        // 注意：EquipmentService.delete(Long id, Long operatorId) 需要两个参数，
        // 而 NL 链路无法提供 operatorId，所以这里到达 service 层后因签名不匹配
        // 返回"执行失败"。这正是 docs/audit 中记录的 NL 写操作待修正项。
    }

    @Test
    @DisplayName("单参读命令持有权限时能真正执行并触达 service")
    void readCommandWithSingleArgReallyExecutes() {
        authenticate("equipment:list");
        Object result = executor.dispatch("equipment", "getById", "EQ-42");
        // 既不是任何一类拒绝，也真的调用了 service(mock 返回 null → 统一"无返回数据")
        assertNotEquals("未登录，无法执行该操作", result);
        assertNotEquals("没有执行该操作的权限：equipment:list", result);
        assertNotEquals("无法识别的实体ID", result);
        assertEquals("执行完成，无返回数据", result);
        verify(equipmentService).getById(42L);
    }

    @Test
    @DisplayName("读命令只需要读权限")
    void readCommandNeedsOnlyReadPermission() {
        authenticate("equipment:list", "nl:execute");
        Object result = executor.dispatch("equipment", "getById", "EQ-42");
        assertNotEquals("未登录，无法执行该操作", result);
        assertNotEquals("没有执行该操作的权限：equipment:list", result);
        verify(equipmentService).getById(42L);
    }

    // ==================== 参数校验 ====================

    @Test
    @DisplayName("非法实体ID被拒绝且不回显原始输入（已授权后）")
    void rejectsMalformedEntityIds() {
        authenticate("equipment:list");
        String[] malicious = {
                "EQ-abc", "EQ-", "EQ-1;DROP TABLE sys_equipment", "EQ--1",
                "EQ-1.5", "EQ-99999999999999999999999", "../../etc/passwd"
        };
        for (String id : malicious) {
            Object result = executor.dispatch("equipment", "getById", id);
            assertEquals("无法识别的实体ID", result, "应拒绝: " + id);
        }
        verifyNoInteractions(equipmentService);
    }

    @Test
    @DisplayName("不需要 ID 的命令传 null 是合法的")
    void nullIdAllowedForIdlessCommands() {
        authenticate("equipment:list");
        assertDoesNotThrow(() -> executor.dispatch("equipment", "getPage", null));
    }

    // ==================== 元数据 ====================

    @Test
    @DisplayName("classify 返回声明的动作分类")
    void classifyActions() {
        assertEquals("READ", NLExecutor.classify("equipment", "getById"));
        assertEquals("DELETE", NLExecutor.classify("equipment", "delete"));
        assertEquals("UNKNOWN", NLExecutor.classify("equipment", "notAMethod"));
    }

    @Test
    @DisplayName("每个命令声明的权限都与其动作类型相称")
    void permissionsMatchActionSeverity() {
        assertEquals("equipment:list", executor.describe("equipment", "getById").permission());
        assertEquals("equipment:del", executor.describe("equipment", "delete").permission());
        assertEquals("consumable:del", executor.describe("consumable", "delete").permission());
        assertEquals("inventory:del", executor.describe("inspection", "delete").permission());
    }
}
