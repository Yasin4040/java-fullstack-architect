# OldProject 重构开源化 Checklist

## 阶段一：基础架构抽象

- [ ] gateway-core-api 模块创建完成
  - [ ] RouteRepository 接口定义完成
  - [ ] ServiceDiscoveryClient 接口定义完成
  - [ ] AuthProvider 接口定义完成
  - [ ] AuthorizationProvider 接口定义完成
  - [ ] 接口文档编写完成

- [ ] gateway-plugin-nacos 模块创建完成
  - [ ] NacosServiceDiscoveryClient 实现完成
  - [ ] NacosRouteRepository 实现完成
  - [ ] 条件装配配置完成
  - [ ] 单元测试通过

- [ ] gateway-plugin-local 模块创建完成
  - [ ] LocalRouteRepository 实现完成
  - [ ] StaticServiceDiscoveryClient 实现完成
  - [ ] 配置文件模板提供
  - [ ] 单元测试通过

## 阶段二：Admin 服务重构

- [ ] gateway-manage 服务重构完成
  - [ ] jtyjy-basic 内部依赖已移除
  - [ ] 硬编码 Nacos 地址已替换
  - [ ] gateway-core-api 已集成
  - [ ] 多存储后端支持已实现
  - [ ] 集成测试通过

- [ ] 认证层抽象完成
  - [ ] OAuth2AuthProvider 实现完成
  - [ ] 公司 OA 登录代码已移除
  - [ ] 标准 JWT 验证支持已添加
  - [ ] 认证流程测试通过

## 阶段三：Gateway 服务重构

- [ ] gateway 服务核心重构完成
  - [ ] gateway-core-api 已集成
  - [ ] AuthorizationManager 已接口化
  - [ ] UserDTO 已抽象为通用模型
  - [ ] 硬编码配置已替换
  - [ ] 集成测试通过

- [ ] 负载均衡策略抽象完成
  - [ ] LoadBalancerStrategy 接口定义完成
  - [ ] DevLoadBalanceRuleLoadBalancer 已重构
  - [ ] 策略配置支持已添加
  - [ ] 负载均衡测试通过

## 阶段四：公共包重构

- [ ] jtyjy-security-oauth 模块重构完成
  - [ ] TokenEnhancer 接口已抽象
  - [ ] 公司特定 JWT 增强逻辑已移除
  - [ ] 标准 JWT 实现已提供
  - [ ] 安全测试通过

- [ ] 公共包清理完成
  - [ ] jtyjy-rbac-starter 内部依赖已移除
  - [ ] UserService/PermissionService 接口已抽象
  - [ ] 公司内部 Nexus 配置已移除
  - [ ] eventbus-rocketmq 已改为可选模块
  - [ ] 所有模块编译通过

## 阶段五：Web 前端重构

- [ ] 前端配置层重构完成
  - [ ] ConfigService 配置服务已实现
  - [ ] 品牌信息已改为配置注入
  - [ ] API 基础路径已可配置
  - [ ] 配置功能测试通过

- [ ] 前端认证层重构完成
  - [ ] AuthService 接口已抽象
  - [ ] 硬编码 CLIENT_ID/CLIENT_SECRET 已移除
  - [ ] 多种登录方式配置支持已添加
  - [ ] 登录流程测试通过

- [ ] 前端路由层重构完成
  - [ ] MenuService 接口已抽象
  - [ ] 静态/动态路由配置支持已添加
  - [ ] 公司特定路由加载逻辑已移除
  - [ ] 路由功能测试通过

## 阶段六：文档和示例

- [ ] 重构文档编写完成
  - [ ] 架构设计文档已完成
  - [ ] 接口扩展指南已完成
  - [ ] 部署配置指南已完成
  - [ ] 文档审核通过

- [ ] 示例项目创建完成
  - [ ] 最小化启动示例可运行
  - [ ] 自定义认证扩展示例可运行
  - [ ] 自定义服务发现扩展示例可运行
  - [ ] 示例文档完整

## 最终验证

- [ ] 代码审查完成
  - [ ] 无公司特定硬编码
  - [ ] 无内部依赖引用
  - [ ] 接口设计合理
  - [ ] 代码质量达标

- [ ] 功能测试完成
  - [ ] Admin 服务功能完整
  - [ ] Gateway 服务功能完整
  - [ ] Web 前端功能完整
  - [ ] 端到端测试通过

- [ ] 开源准备完成
  - [ ] LICENSE 文件添加
  - [ ] README 文档完善
  - [ ] 贡献指南编写
  - [ ] 代码仓库清理
