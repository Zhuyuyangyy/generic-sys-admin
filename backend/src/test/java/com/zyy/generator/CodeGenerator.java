package com.zyy.generator;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;
import com.zyy.model.entity.BaseEntity;
import com.zyy.common.BaseController;
import com.zyy.common.Result;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.Function;

/**
 * ==========================================================
 * 🏆 毕设工厂核心：MyBatis-Plus 一键代码生成器
 * ==========================================================
 *
 * 🎯 功能：连接数据库，一键生成：
 * - Entity（实体类）     → 继承 BaseEntity（自动获得雪花ID+逻辑删除+时间戳）
 * - Mapper（数据访问层） → 继承 BaseMapper
 * - Service（业务层）    → 一行代码完成 CRUD
 * - Controller（控制器） → 继承 BaseController（自动获得 success/error 方法）
 * - Mapper.xml（SQL映射）→ MyBatis SQL 配置文件
 *
 * 🎓 适用场景：
 * - 接到任何新业务（比如图书管理、订单管理、员工管理）
 * - 只需改几个配置，30秒生成全套后端代码
 * - 然后你只需要写业务逻辑，其他重复代码自动生成
 *
 * 📖 使用方法（只需要改这5个地方）：
 * ```java
 * public class CodeGeneratorTest {
 *     @Test
 *     public void generate() {
 *         CodeGenerator.generate(
 *             "jdbc:mysql://localhost:3306/数据库名",
 *             "root",           // 数据库用户名
 *             "123456",         // 数据库密码
 *             "com.zyy.module.你的模块名",  // 包路径，比如 book
 *             "你的表名"        // 比如 book（会生成 Book.java）
 *         );
 *     }
 * }
 * ```
 *
 * ⚠️ 注意：
 * 1. 先在数据库建好表！
 * 2. 表名建议用下划线命名（book_info），类名会自动转驼峰（BookInfo）
 * 3. 生成前确认 application.yml 数据库连接正确
 *
 * @author Alice
 */
public class CodeGenerator {

    /**
     * 一键生成代码的主方法 🎯
     *
     * @param jdbcUrl  数据库连接URL
     * @param username 数据库用户名
     * @param password 数据库密码
     * @param packageName 包名路径（建议用模块名，如 book / order / user）
     * @param tableName 表名（实体类会根据此表自动生成）
     */
    public static void generate(String jdbcUrl, String username, String password,
                                String packageName, String tableName) {
        // 打印一个醒目的分隔线，方便在控制台找到输出位置
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║     🎓 毕设工厂 · MyBatis-Plus 代码生成器  ·  开始运行     ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("📦 包路径：" + packageName);
        System.out.println("📋 数据表：" + tableName);
        System.out.println("🔗 数据库：" + jdbcUrl);
        System.out.println("\n");

        /**
         * ========== MyBatis-Plus Generator 核心配置 ==========
         *
         * FastAutoGenerator.create() 是官方推荐的链式调用写法
         * 依次配置：数据源 → 全局配置 → 包配置 → 模板配置 → 注入配置 → 生成策略
         */
        FastAutoGenerator fastAutoGenerator = FastAutoGenerator.create(jdbcUrl, username, password);
        fastAutoGenerator.globalConfig(builder -> {
            builder
                    /**
                     * author：生成的代码顶部会标注作者名
                     * 这里写死 "Generated" 作为标记，小白也可以改成自己的名字
                     */
                    .author("Generated")

                    /**
                     * dateType：时间类型使用 LocalDateTime（Java8新时间API）
                     * 相比 Date 更安全，更现代化
                     */
                    .dateType(DateType.TIME_PACK)

                    /**
                     * commentDate：注释里的日期格式
                     */
                    .commentDate("yyyy-MM-dd HH:mm:ss");

            /**
             * ⚠️ 关闭生成 Swagger 注解开关（可选）
             * 如果项目不需要 Swagger 文档，关掉可以减少代码量
             * 如果需要，开启即可：.swagger2(true)
             */
            // Swagger配置已移除，如需启用请确保依赖正确
        });
        fastAutoGenerator.packageConfig(builder -> {
            builder
                    /**
                     * parent：所有生成文件的父包路径
                     * 比如填 com.zyy.module.book
                     * 就会生成：
                     * - com.zyy.module.book.entity.Book.java
                     * - com.zyy.module.book.mapper.BookMapper.java
                     * - com.zyy.module.book.service.BookService.java
                     * - com.zyy.module.book.controller.BookController.java
                     */
                    .parent(packageName)

                    /**
                     * entity：实体类包的子包名
                     */
                    .entity("entity")

                    /**
                     * mapper：Mapper接口的子包名
                     */
                    .mapper("mapper")

                    /**
                     * service：Service接口和实现类的子包名
                     * 注意：ServiceImpl 会在 service.impl 包下
                     */
                    .service("service")
                    .serviceImpl("service.impl")

                    /**
                     * controller：Controller的子包名
                     */
                    .controller("controller")

                    /**
                     * xml：Mapper.xml 文件的存放路径
                     * 注意！这是相对于项目根目录的路径
                     * 如果想让 xml 放到 resources/mapper/ 下，需要配合 builder.pathInfo() 设置
                     */
                    .pathInfo(Collections.singletonMap(
                            OutputFile.xml,
                            System.getProperty("user.dir") + "/src/main/resources/mapper"
                    ));
        });
        fastAutoGenerator.templateConfig(builder -> {
            builder
                    /**
                     * 禁用模板引擎（使用默认模板，不需要额外配置 ftl 文件）
                     * 如果你想自定义生成的模板，可以编写 .ftl 文件并在这里配置路径
                     */
                    .disable(TemplateType.CONTROLLER, TemplateType.SERVICE_IMPL)
            /**
             * 设置自定义模板路径（可选）
             * 如果你需要完全自定义生成代码的格式，可以准备自己的 ftl 模板
             * .template(new FileTemplateConfig().setEntity("/templates/entity.ftl"));
             */
            ;
        });
        fastAutoGenerator.strategyConfig(builder -> {
            builder
                    /**
                     * addInclude：设置要生成的表名
                     * 支持多表：.addInclude("user", "role", "menu")
                     * 支持通配符：.addInclude("sys_.*") 生成所有 sys_ 开头的表
                     */
                    .addInclude(tableName)

                    /**
                     * ⚠️ entityName：Entity 实体类命名策略
                     * naming = underline_to_camel：将表的下划线命名转驼峰
                     * 比如 book_info → BookInfo, sys_user → SysUser
                     * tolineName = underline_to_camel：将字段的下划线转驼峰
                     */
                    .entityBuilder()
                    .enableLombok()      // 启用 Lombok（自动生成 getter/setter）
                    .enableTableFieldAnnotation()  // 给字段添加 @TableField 注解
                    .enableFileOverride()          // 如果文件存在，覆盖它（方便重复生成）

                    /**
                     * ⚠️ naming / tolineName：命名策略
                     * 配置正确的话，表字段会自动映射到 Java 属性
                     * book_id → Book.id, create_time → Book.createTime
                     */
                    .naming(NamingStrategy.underline_to_camel)
                    .columnNaming(NamingStrategy.underline_to_camel)

                    /**
                     * 🏆 高级创新点：让 Entity 继承 BaseEntity
                     * 继承后自动拥有：雪花ID主键、逻辑删除、createTime/updateTime自动填充
                     * 不需要手动写这些字段！
                     */
                    .superClass(BaseEntity.class)

                    /**
                     * idType：主键生成策略
                     * IdType.ASSIGN_ID = 雪花算法（推荐）
                     * IdType.AUTO = 数据库自增（需要数据库支持 AUTO_INCREMENT）
                     * IdType.INPUT = 手动输入（不推荐）
                     */
                    .idType(IdType.ASSIGN_ID)

                    // ==================== 第五步：Mapper 生成策略 ====================
                    .mapperBuilder()
                    .enableBaseResultMap()   // 生成 ResultMap（用于关联查询）
                    .enableBaseColumnList()  // 生成 SQL 片段（SELECT id, name...）
                    .enableFileOverride()    // 覆盖已有文件
                    .superClass(BaseMapper.class)  // 继承 BaseMapper<实体类>
                    .formatMapperFileName("%sMapper")  // 文件名格式：BookMapper

                    // ==================== 第六步：Service 生成策略 ====================
                    .serviceBuilder()
                    .enableFileOverride()    // 覆盖已有文件
                    .formatServiceFileName("I%sService")     // 接口名格式：IBookService
                    .formatServiceImplFileName("%sServiceImpl")  // 实现类名：BookServiceImpl
                    // ==================== 第七步：Controller 生成策略（重点！） ====================
                    .controllerBuilder()
                    .enableRestStyle()    // 生成 @RestController（返回JSON，不是视图）
                    .enableFileOverride() // 覆盖已有文件

                    /**
                     * 🏆 高级创新点：让 Controller 继承 BaseController
                     * 继承后自动拥有：
                     * - success() / success(data) / success(data, msg)
                     * - error() / error(msg) / error(code, msg)
                     * 不需要手动写 Result.ok() 了！
                     *
                     * 同时保留父类的 @RestController 注解
                     */
                    .superClass(BaseController.class)
            ;
        });
        fastAutoGenerator.templateEngine(new FreemarkerTemplateEngine());
        fastAutoGenerator.execute();// ==================== 第一步：全局配置 ====================
// ==================== 第二步：包配置 ====================
// ==================== 第三步：模板配置 ====================
// ==================== 第四步：Entity 生成策略（重点！） ====================
/**
 * ========== 最后一步：执行生成 ==========
 *
 * 使用 Freemarker 模板引擎（需要引入 freemarker 依赖）
 * 如果不想用模板引擎：
 * 改为 .templateEngine(new MybatisPlusTemplateEngine()).execute();
 */

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║     🎓 代码生成完毕！请查看上述输出目录中的文件              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("\n");
    }

    // ==================== 以下是高级辅助方法 ====================

    /**
     * 将下划线命名转为首字母大写的驼峰命名
     * 例如：book_info → BookInfo
     */
    private static String naming(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

    // ==================== 业务示范：如何30秒生成图书管理系统 ====================

    /**
     * ==========================================================
     * 🎓 业务示范：图书管理系统 Book 代码生成
     * ==========================================================
     *
     * 假设你现在要做一个"图书管理系统"，数据库里有一张表叫 book
     *
     * 建表SQL：
     * ```sql
     * CREATE TABLE book (
     *     id          BIGINT PRIMARY KEY,
     *     name        VARCHAR(128)  NOT NULL COMMENT '书名',
     *     author      VARCHAR(64)   COMMENT '作者',
     *     publish_yr  INT           COMMENT '出版年份',
     *     isbn        VARCHAR(32)   COMMENT 'ISBN书号',
     *     price       DECIMAL(10,2) COMMENT '定价',
     *     stock       INT           COMMENT '库存数量',
     *     create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
     *     update_time DATETIME      ON UPDATE CURRENT_TIMESTAMP,
     *     is_deleted  TINYINT       DEFAULT 0
     * );
     * ```
     *
     * 然后运行这个测试方法，即可生成 Book.java, BookMapper.java, BookService.java, BookController.java
     */
    @Test
    public void generateBookModule() {
        // 连接到本地 MySQL 的 book_db 数据库
        String jdbcUrl = "jdbc:mysql://localhost:3306/generic_sys_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false";
        String username = "root";
        String password = "123456";
        String packageName = "com.zyy.module.book";
        String tableName = "sys_menu";  // 这里用现成的 sys_menu 表演示，实际换成 book 表

        System.out.println("🎓 正在为 [图书管理系统] 生成代码...");
        System.out.println("📌 实际使用时，把 tableName 换成 'book' 即可\n");

        generate(jdbcUrl, username, password, packageName, tableName);
    }

    /**
     * 批量生成示例（一次生成多个表）
     *
     * 用法：把要生成的表名放到数组里，一起生成
     */
    @Test
    public void generateMultipleTables() {
        String jdbcUrl = "jdbc:mysql://localhost:3306/generic_sys_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false";
        String username = "root";
        String password = "123456";
        String packageName = "com.zyy.module.book";

        String[] tables = {"book", "reader", "borrow_record", "category"};

        for (String table : tables) {
            System.out.println("\n========== 正在生成：【" + table + "】模块 ==========\n");
            generate(jdbcUrl, username, password, packageName, table);
        }
    }

    /**
     * 完整配置示例（用于参考）
     *
     * 这个方法展示了所有可配置项，新手可以参考这个来调整
     */
    @Test
    public void generateFullExample() {
        // ===== 配置区 =====
        String jdbcUrl = "jdbc:mysql://localhost:3306/generic_sys_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false";
        String username = "root";
        String password = "123456";
        String packageName = "com.zyy.module.example";
        String tableName = "sys_user";

        System.out.println("🎓 完整配置示例运行中...");
        System.out.println("📌 提示：如果想修改生成内容，参考本类中的各 strategyConfig 配置项\n");

        // ===== 生成逻辑 =====
        generate(jdbcUrl, username, password, packageName, tableName);

        System.out.println("✅ 生成完成！");
        System.out.println("📁 建议检查以下目录是否有文件生成：");
        System.out.println("   - src/main/java/" + packageName.replace('.', '/') + "/entity/");
        System.out.println("   - src/main/java/" + packageName.replace('.', '/') + "/mapper/");
        System.out.println("   - src/main/java/" + packageName.replace('.', '/') + "/service/");
        System.out.println("   - src/main/java/" + packageName.replace('.', '/') + "/controller/");
        System.out.println("   - src/main/resources/mapper/");
    }
}
