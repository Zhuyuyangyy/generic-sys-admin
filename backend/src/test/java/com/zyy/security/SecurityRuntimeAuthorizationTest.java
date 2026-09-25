package com.zyy.security;

import com.zyy.controller.InventoryRecordController;
import com.zyy.controller.NLController;
import com.zyy.controller.RoleController;
import com.zyy.controller.SysUserController;
import com.zyy.model.dto.InventoryRecordSaveDTO;
import com.zyy.nl.CausalDAGService;
import com.zyy.nl.NLService;
import com.zyy.service.ConsumableService;
import com.zyy.service.EquipmentService;
import com.zyy.service.InventoryRecordService;
import com.zyy.service.RbacService;
import com.zyy.service.SysMenuService;
import com.zyy.service.SysOperationLogService;
import com.zyy.service.SysRoleService;
import com.zyy.service.SysUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 证明 Spring Security 链真的在工作。
 *
 * 断言"权限字符串存在于 seed"只能说明 annotation 与数据库一致；这里走完整链路：
 *
 * <pre>
 * MockMvc → Security Filter Chain → Authentication → Method Security → Controller
 * </pre>
 *
 * 覆盖四类代表接口 × 三种身份（anonymous / 错权限 / 对权限）。业务 Service 全部
 * mock，被测对象是授权边界，不是业务逻辑。
 *
 * HTTP 语义：anonymous → 401，已认证但权限不足 → 403，放行 → 200。
 */
@SpringBootTest(
        properties = {
                "spring.redis.enabled=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.url=jdbc:h2:mem:secruntime;MODE=MySQL",
                "spring.sql.init.mode=never",
                "mybatis-plus.mapper-locations=",
                "minio.endpoint=http://localhost:99991",
                "tts.provider=mock"
        }
)
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("Spring Security 运行时授权测试")
class SecurityRuntimeAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 构造带指定 authority 的 Authentication，逐请求注入，避免 context 缓存干扰。
     * principal 用真实的 LoginUser——Controller 通过 SecurityUtils.currentUserId()
     * 取 operatorId，principal 不是 LoginUser 会抛 UnauthorizedException（401）。
     */
    private static org.springframework.security.authentication.UsernamePasswordAuthenticationToken
    tokenWith(String authority) {
        com.zyy.security.LoginUser principal = new com.zyy.security.LoginUser(
                1L, "tester",
                java.util.List.of(new org.springframework.security.core.authority
                        .SimpleGrantedAuthority(authority)));
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null,
                java.util.List.of(new org.springframework.security.core.authority
                        .SimpleGrantedAuthority(authority)));
    }

    @MockBean
    private SysUserService sysUserService;

    @MockBean
    private SysRoleService sysRoleService;

    @MockBean
    private InventoryRecordService inventoryRecordService;

    @MockBean
    private SysMenuService sysMenuService;

    @MockBean
    private SysOperationLogService sysOperationLogService;

    @MockBean
    private RbacService rbacService;

    @MockBean
    private NLService nlService;

    @MockBean
    private CausalDAGService causalDAGService;

    /** @WebSecurityContext 需要它来构造 filter；真实解析由 post-processor 接管。 */
    @MockBean
    private com.zyy.security.JwtUtil jwtUtil;

    /** Controller 通过它取 operatorId；mock 后返回 null，不影响授权判定。 */

    @MockBean
    private EquipmentService equipmentService;

    @MockBean
    private ConsumableService consumableService;

    private static final String VALID_USER_JSON =
            "{\"username\":\"alice\",\"password\":\"secret123\",\"realName\":\"Alice\"}";

    // ==================== 1. 用户管理 ====================

    @Test
    @DisplayName("用户删除：anonymous → 401")
    void userDeleteAnonymous() throws Exception {
        mockMvc.perform(delete("/api/users/9"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("用户删除：只有 list → 403")
    void userDeleteWithWrongAuthority() throws Exception {
        mockMvc.perform(delete("/api/users/9").with(authentication(tokenWith("system:user:list"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("用户删除：持有 del → 200 并进入 Controller")
    void userDeleteWithCorrectAuthority() throws Exception {
        mockMvc.perform(delete("/api/users/9").with(authentication(tokenWith("system:user:del"))))
                .andExpect(status().isOk());
        verify(sysUserService).delete(9L, 1L);
    }

    @Test
    @DisplayName("用户列表：只有 system:user:list 可读")
    void userListAuthorization() throws Exception {
        when(sysUserService.getPage(anyLong(), anyLong(), any(), any())).thenReturn(null);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users").with(authentication(tokenWith("system:role:list"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users").with(authentication(tokenWith("system:user:list"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("用户创建：持有 add → 200")
    void userCreateWithCorrectAuthority() throws Exception {
        when(sysUserService.register(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/users")
                        .with(authentication(tokenWith("system:user:add")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("用户创建：只有 list → 403")
    void userCreateWithWrongAuthority() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(authentication(tokenWith("system:user:list")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isForbidden());

        verify(sysUserService, never()).register(any(), any());
    }

    @Test
    @DisplayName("用户创建：anonymous → 401")
    void userCreateAnonymous() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 2. 角色 / 权限管理 ====================

    @Test
    @DisplayName("角色授权菜单：system:role:grant 专属，list 不能提权")
    void roleGrantAuthorization() throws Exception {
        mockMvc.perform(put("/api/roles/3/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/roles/3/menus")
                        .with(authentication(tokenWith("system:role:list")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/roles/3/menus")
                        .with(authentication(tokenWith("system:role:grant")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2,3]"))
                .andExpect(status().isOk());
        verify(sysRoleService).grantMenus(3L, List.of(1L, 2L, 3L));
    }

    @Test
    @DisplayName("角色删除：持有 del → 200")
    void roleDeleteWithCorrectAuthority() throws Exception {
        mockMvc.perform(delete("/api/roles/4").with(authentication(tokenWith("system:role:del"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("角色删除：只有 add → 403")
    void roleDeleteWithWrongAuthority() throws Exception {
        mockMvc.perform(delete("/api/roles/4").with(authentication(tokenWith("system:role:add"))))
                .andExpect(status().isForbidden());

        verify(sysRoleService, never()).delete(anyLong(), any());
    }

    // ==================== 3. inventory 写操作 ====================

    private static final String VALID_RECORD_JSON =
            "{\"equipmentId\":1,\"recordType\":\"ROUTINE\",\"recordDate\":\"2026-01-01\"}";

    @Test
    @DisplayName("巡检记录创建：持有 add → 200")
    void inventoryCreateWithCorrectAuthority() throws Exception {
        when(inventoryRecordService.save(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/inventory-records")
                        .with(authentication(tokenWith("inventory:add")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RECORD_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("巡检记录创建：只有 list → 403（回归读写混用）")
    void inventoryCreateWithWrongAuthority() throws Exception {
        mockMvc.perform(post("/api/inventory-records")
                        .with(authentication(tokenWith("inventory:list")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RECORD_JSON))
                .andExpect(status().isForbidden());

        verify(inventoryRecordService, never()).save(any(), any());
    }

    @Test
    @DisplayName("巡检记录删除：需要 inventory:del，ROLE_ADMIN 不再是逃生口")
    void inventoryDeleteRequiresDelAuthority() throws Exception {
        mockMvc.perform(delete("/api/inventory-records/5"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/inventory-records/5")
                        .with(authentication(tokenWith("inventory:list"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/inventory-records/5")
                        .with(authentication(tokenWith("inventory:del"))))
                .andExpect(status().isOk());
        verify(inventoryRecordService).delete(5L, 1L);
    }

    // ==================== 4. NL execute ====================

    @Test
    @DisplayName("NL 执行：持有 nl:execute → 200")
    void nlExecuteWithCorrectAuthority() throws Exception {
        when(nlService.execute(any())).thenReturn("ok");

        mockMvc.perform(post("/api/nl/execute")
                        .with(authentication(tokenWith("nl:execute")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"查询设备 EQ-1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("NL 执行：只有 equipment:list → 403（NL 不是设备权限的别名）")
    void nlExecuteWithWrongAuthority() throws Exception {
        mockMvc.perform(post("/api/nl/execute")
                        .with(authentication(tokenWith("equipment:list")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"查询设备 EQ-1\"}"))
                .andExpect(status().isForbidden());

        verify(nlService, never()).execute(any());
    }

    @Test
    @DisplayName("NL 因果执行接口同样要求 nl:execute，equipment:del 不够")
    void nlCausalExecuteAuthorization() throws Exception {
        mockMvc.perform(post("/api/nl/execute-with-causal-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"删除设备 EQ-1\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/nl/execute-with-causal-check")
                        .with(authentication(tokenWith("equipment:del")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"删除设备 EQ-1\"}"))
                .andExpect(status().isForbidden());

        verify(nlService, never()).executeWithCausalCheck(any());
    }

    // ==================== 公开端点 ====================

    @Test
    @DisplayName("登录接口保持公开（匿名可访问）")
    void publicAuthEndpointRemainsPublic() throws Exception {
        when(sysUserService.login(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("用户列表不是公开端点（回归 /api/users permitAll 漏洞）")
    void userListIsNotPublic() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 库存幂等：HTTP 语义 ====================

    @Test
    @DisplayName("无 Idempotency-Key 的入库请求正常放行（向后兼容）")
    void inboundWithoutKeyStillWorks() throws Exception {
        mockMvc.perform(post("/api/consumables/7/inbound")
                        .with(authentication(tokenWith("consumable:in")))
                        .param("quantity", "10"))
                .andExpect(status().isOk());
        verify(consumableService).inbound(eq(7L), eq(10), any(), any(), eq(1L), isNull());
    }

    @Test
    @DisplayName("携带 Idempotency-Key 时它被透传到业务层")
    void inboundKeyIsForwarded() throws Exception {
        mockMvc.perform(post("/api/consumables/7/inbound")
                        .with(authentication(tokenWith("consumable:in")))
                        .header("Idempotency-Key", "client-key-1")
                        .param("quantity", "10"))
                .andExpect(status().isOk());
        verify(consumableService).inbound(eq(7L), eq(10), any(), any(), eq(1L), eq("client-key-1"));
    }

    @Test
    @DisplayName("幂等键被不同 payload 复用 → 409")
    void inboundKeyConflictReturns409() throws Exception {
        doThrow(new com.zyy.service.InventoryIdempotencyService
                .IdempotencyKeyConflictException("Idempotency-Key 已被不同请求使用"))
                .when(consumableService).inbound(any(), anyInt(), any(), any(), any(), any());

        mockMvc.perform(post("/api/consumables/7/inbound")
                        .with(authentication(tokenWith("consumable:in")))
                        .header("Idempotency-Key", "reused-key")
                        .param("quantity", "999"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("幂等重放返回 200 且不再调用业务层")
    void inboundReplayReturnsOkWithoutSecondMutation() throws Exception {
        doThrow(new com.zyy.service.impl.ConsumableServiceImpl.ReplayedRequestException("INBOUND:7:10"))
                .when(consumableService).inbound(any(), anyInt(), any(), any(), any(), any());

        mockMvc.perform(post("/api/consumables/7/inbound")
                        .with(authentication(tokenWith("consumable:in")))
                        .header("Idempotency-Key", "same-key")
                        .param("quantity", "10"))
                .andExpect(status().isOk());
        // 只调用一次；重放由 Controller 捕获，不再执行业务
        verify(consumableService, times(1))
                .inbound(any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("出库与调拨同样接受 Idempotency-Key")
    void outboundAndAdjustAcceptKey() throws Exception {
        mockMvc.perform(post("/api/consumables/7/outbound")
                        .with(authentication(tokenWith("consumable:out")))
                        .header("Idempotency-Key", "out-1")
                        .param("quantity", "3"))
                .andExpect(status().isOk());
        verify(consumableService).outbound(eq(7L), eq(3), any(), any(), eq(1L), eq("out-1"));

        mockMvc.perform(patch("/api/consumables/7/stock")
                        .with(authentication(tokenWith("consumable:edit")))
                        .header("Idempotency-Key", "adj-1")
                        .param("delta", "-5"))
                .andExpect(status().isOk());
        verify(consumableService).adjustStock(eq(7L), eq(-5), any(), any(), eq(1L), eq("adj-1"));
    }
}
