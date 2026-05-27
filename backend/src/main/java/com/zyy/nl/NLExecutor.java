package com.zyy.nl;

import com.zyy.service.EquipmentService;
import com.zyy.service.ConsumableService;
import com.zyy.service.InventoryRecordService;
import com.zyy.service.SysUserService;
import com.zyy.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 自然语言执行器。
 * <p>通过Java反射调用已有的Service方法。</p>
 *
 * @author ZYY Agent
 * @since Java 17
 */
@Component
public class NLExecutor {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private ConsumableService consumableService;

    @Autowired
    private InventoryRecordService inspectionService;

    @Autowired
    private SysUserService userService;

    @Autowired
    private SysRoleService roleService;

    /**
     * 反射调度Service方法。
     *
     * @param serviceMethod 方法名（如 "getEquipmentById"）
     * @param entityId       实体ID（如 "EQ-1"），无实体时传null
     * @return 方法返回值；不支持时返回null
     */
    public Object dispatch(String serviceMethod, String entityId) {
        try {
            // 1. 分离ID数字
            String numericId = null;
            if (entityId != null && entityId.contains("-")) {
                numericId = entityId.substring(entityId.indexOf('-') + 1);
            }

            // 2. 尝试各Service
            if (serviceMethod.startsWith("getEquipment") || serviceMethod.startsWith("createEquipment")
                    || serviceMethod.startsWith("updateEquipment") || serviceMethod.startsWith("deleteEquipment")
                    || serviceMethod.startsWith("listEquipments") || serviceMethod.startsWith("countEquipments")) {
                return call(equipmentService, serviceMethod, numericId);
            }
            if (serviceMethod.startsWith("getConsumable") || serviceMethod.startsWith("createConsumable")
                    || serviceMethod.startsWith("updateConsumable") || serviceMethod.startsWith("deleteConsumable")
                    || serviceMethod.startsWith("countConsumables")) {
                return call(consumableService, serviceMethod, numericId);
            }
            if (serviceMethod.startsWith("getInspection") || serviceMethod.startsWith("createInspection")
                    || serviceMethod.startsWith("updateInspection") || serviceMethod.startsWith("deleteInspection")
                    || serviceMethod.startsWith("countInspections")) {
                return call(inspectionService, serviceMethod, numericId);
            }

        } catch (Exception e) {
            return "调用异常：" + e.getMessage();
        }
        return null;
    }

    private Object call(Object svc, String methodName, String arg) throws Exception {
        if (arg == null) {
            // 无参数方法（如 listEquipments、countEquipments）
            Method m = svc.getClass().getMethod(methodName);
            return m.invoke(svc);
        } else {
            Method m = svc.getClass().getMethod(methodName, Long.class);
            return m.invoke(svc, Long.parseLong(arg));
        }
    }
}