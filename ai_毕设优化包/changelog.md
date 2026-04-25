# CHANGELOG - 版本更新记录

---

## [1.0.0] 2026-04-19 - 初始版本

### 🎉 首次发布

#### 新增功能

**后端核心**
- SpringBoot3 主启动类 + @MapperScan配置
- MyBatis-Plus集成（BaseEntity、BaseController、PageParam）
- JWT无状态认证（JwtUtil、JwtAuthFilter、LoginUser）
- RBAC权限模型（SysUser、SysRole、SysMenu实体）
- AOP操作日志切面（OperationLogAspect）
- 全局异常处理器（GlobalExceptionHandler）
- 4个业务异常类（BusinessException、UnauthorizedException、ForbiddenException、ResourceNotFoundException）
- 通用响应结构（Result、ResultCode）
- MyBatis-Plus代码生成器（CodeGenerator）
- EasyExcel导出工具（EasyExcelUtil）

**存储模块**
- MinIO分布式对象存储（MinioConfig、MinioUtil）
- MinioAvailability可用性状态Bean
- LocalFileStorageStrategy本地降级策略
- FileController（单上传/批量上传/删除）
- MinIO Docker Compose一键启动配置

**语音模块**
- MinimaxTTS文字转语音（MinimaxTtsUtil）
- TtsService接口抽象（策略模式）
- MockTtsServiceImpl降级实现
- TtsController（broadcast + status）

**配置**
- application.yml/dev/prod多环境配置
- Knife4j OpenAPI3文档配置（含JWT全局认证）
- 全局响应示例定制器（Mock数据免Key调试）

**前端**
- Vue3 + Element Plus + Pinia项目结构
- Axios请求封装（axios.ts、request.ts）
- TTS接口封装（tts.ts）
- 通用CRUD模板（CrudTemplate.vue）
- 通用仪表盘模板（DashboardTemplate.vue）
- 动态菜单组件（DynamicMenu.vue）
- ECharts图表组件（Line/Bar/Pie/3D）
- Three.js 3D查看器（ThreeDViewer.vue）
- DashboardView完整仪表盘页面

#### 文档
- MySQL 7张核心表建表SQL
- 毕业论文大纲（含完整7章结构）
- 答辩PPT大纲（12页）
- 本地启动说明
- MinIO快速安装指南

#### 脚本
- check_env.bat Windows环境检测脚本
- minio-docker-compose.yml MinIO容器编排

---

## [1.1.0] 计划中

### 待实现

- [ ] Redis缓存层集成
- [ ] WebSocket实时日志推送
- [ ] 单元测试覆盖率提升
- [ ] Docker容器化部署
- [ ] Spring Cloud微服务架构扩展
- [ ] 移动端H5适配
- [ ] 低代码配置平台

---

## 版本号规范

本项目采用 **语义化版本（Semantic Versioning）**：

```
主版本号.次版本号.修订号
  1   .   0   .   0

主版本号：重大架构变更，不兼容的API变更
次版本号：功能新增，保持向后兼容
修订号：bug修复、文档更新、小优化
```

---

*Changelog遵循 [Keep a Changelog](https://keepachangelog.com/) 规范*
