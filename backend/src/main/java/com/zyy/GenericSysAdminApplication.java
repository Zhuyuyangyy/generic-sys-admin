package com.zyy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ==========================================================
 * 通用系统管理平台 - 数据库初始化脚本
 * 1. MySQL 连接配置：JDBC url 中?3306 MySQL 默认端口
 * 2. 创建数据库：generic_sys_admin 数据库名，初始化脚本位于 sql/v1.0__init.sql
 * 3. application.yml 中配置 DB_PASSWORD 数据库密码，数据库用户请根据实际情况修改
 */
@SpringBootApplication
@MapperScan("com.zyy.mapper")
public class GenericSysAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(GenericSysAdminApplication.class, args);
    }
}
