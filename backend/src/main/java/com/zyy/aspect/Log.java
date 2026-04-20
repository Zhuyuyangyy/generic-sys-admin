package com.zyy.aspect;

import com.zyy.enums.BusinessType;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标注在 Controller 方法上，自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /** 操作模块，如"用户管理"、"设备管理" */
    String module() default "";

    /** 操作类型 */
    BusinessType operation() default BusinessType.OTHER;

    /** 操作描述 */
    String description() default "";
}
