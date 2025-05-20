# 车辆电池监控系统文档

## 1. 架构设计

### 1.1 系统概述

本系统是一个基于Spring Boot开发的车辆电池监控系统，用于实时采集、分析和管理车辆电池信号数据，及时识别潜在的电池问题并生成预警信息。该系统采用标准化的API设计，支持高并发数据处理，并提供灵活的数据过滤和查询功能。

### 1.2 技术栈

- **后端框架**: Spring Boot
- **数据访问**: MyBatis-Plus
- **缓存技术**: Redis
- **数据库**: MySQL
- **API文档**: Swagger
- **日志框架**: Logback (通过SLF4J接口)

### 1.3 核心组件

系统由以下几个核心组件构成：

1. **控制层 (Controller)**:
   - 标准CRUD控制器: VehicleController、BatterySignalController、WarnRuleController、WarnInfoController
   - 专用业务控制器: ReportWarnController

2. **服务层 (Service)**:
   - 业务逻辑处理
   - 事务管理
   - 缓存管理

3. **数据访问层 (Mapper)**:
   - 基于MyBatis-Plus的数据库操作
   - 自定义SQL查询

4. **实体层 (Entity)**:
   - 数据库实体映射
   - 数据传输对象 (DTO)

5. **配置层 (Config)**:
   - 全局异常处理
   - Redis配置
   - 系统配置

### 1.4 系统架构图

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  客户端应用  │ <──> │    API 层   │ <──> │  控制器层   │
└─────────────┘      └─────────────┘      └─────────────┘
                                                │
                                                ▼
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   数据库    │ <──> │  数据访问层  │ <──> │   服务层    │
└─────────────┘      └─────────────┘      └─────────────┘
       │                                        │
       │                                        ▼
       │                               ┌─────────────────┐
       └──────────────────────────────>│     缓存层     │
                                       └─────────────────┘
```

## 2. 功能设计

### 2.1 核心功能

#### 2.1.1 车辆管理

- 车辆信息的创建、查询、更新和删除
- 支持批量操作
- 支持基于多种条件的动态查询

#### 2.1.2 电池信号管理

- 电池信号数据的上报与存储
- 信号数据的解析与处理
- 历史数据查询与管理

#### 2.1.3 预警规则管理

- 预警规则的定义与配置
- 规则的启用、禁用与删除
- 规则优先级管理

#### 2.1.4 预警信息管理

- 基于规则的预警生成
- 预警信息的查询与处理
- 预警状态的跟踪与更新

#### 2.1.5 报告与预警专用功能

- 信号处理与预警生成
- 批量信号上报
- 基于多条件的预警信息查询

### 2.2 API设计

#### 2.2.1 标准CRUD接口

每个主要实体（车辆、电池信号、预警规则、预警信息）均提供以下标准接口：

1. **查询 (GET)**:
   - 路径: `/api/{实体}`
   - 参数: 支持动态过滤条件、分页参数
   - 返回: 分页结果集

2. **创建 (POST)**:
   - 路径: `/api/{实体}`
   - 请求体: 实体数据（单个或列表）
   - 返回: 创建的实体数据

3. **更新 (PUT)**:
   - 路径: `/api/{实体}`
   - 请求体: 更新的实体数据（单个或列表）
   - 返回: 操作结果

4. **删除 (DELETE)**:
   - 路径: `/api/{实体}`
   - 参数: ID列表（逗号分隔）
   - 返回: 操作结果

#### 2.2.2 专用业务接口

1. **电池信号处理与预警生成**:
   - 路径: `/api/warn`
   - 方法: POST
   - 功能: 上报电池信号并生成预警

2. **电池信号上报**:
   - 路径: `/api/report`
   - 方法: POST
   - 功能: 批量上报电池信号

3. **预警信息查询**:
   - 路径: `/api/search`
   - 方法: GET
   - 功能: 基于多条件查询预警信息

### 2.3 数据模型

#### 2.3.1 车辆（Vehicle）

- id: 主键
- carId: 车辆ID
- vid: 车辆唯一标识码
- name: 车辆名称
- status: 车辆状态
- createTime: 创建时间
- updateTime: 更新时间

#### 2.3.2 电池信号（BatterySignal）

- id: 主键
- carId: 车辆ID
- warnId: 预警标识
- signalData: 信号数据（JSON格式）
- signalTime: 信号时间
- processed: 是否已处理
- processTime: 处理时间
- createTime: 创建时间

#### 2.3.3 预警规则（WarnRule）

- id: 主键
- name: 规则名称
- description: 规则描述
- condition: 规则条件
- level: 预警等级
- enabled: 是否启用
- priority: 规则优先级
- createTime: 创建时间
- updateTime: 更新时间

#### 2.3.4 预警信息（WarnInfo）

- id: 主键
- carId: 车辆ID
- warnId: 预警ID
- signalId: 信号ID
- warnTime: 预警时间
- warnLevel: 预警等级
- warnDesc: 预警描述
- handled: 是否已处理
- handleTime: 处理时间
- createTime: 创建时间

## 3. 实现细节

### 3.1 控制器标准化

系统对所有主要实体控制器进行了标准化改造，使其遵循统一的接口设计模式：

1. **统一的请求映射**:
   - 使用RestController注解
   - 基于实体的URL路径
   - HTTP方法与操作类型的一致映射

2. **一致的参数处理**:
   - 查询接口支持通用的动态过滤条件
   - 创建和更新接口支持批量操作
   - 删除接口支持批量处理

3. **标准的返回格式**:
   - 使用BaseResponse封装所有响应
   - 统一的成功/失败标识
   - 规范化的错误码和消息

### 3.2 缓存管理

系统采用Redis作为缓存层，提高数据访问效率：

1. **缓存策略**:
   - 频繁访问的数据（如最新电池信号）优先缓存
   - 设置合理的缓存过期时间
   - 使用分布式锁防止缓存击穿

2. **缓存一致性**:
   - 更新操作时自动清除相关缓存
   - 使用延迟删除策略减少缓存抖动
   - 双重检查锁保证线程安全

### 3.3 事务管理

系统对关键业务操作实施严格的事务控制：

1. **注解式事务**:
   - 使用@Transactional注解管理事务
   - 为不同操作设置合适的事务传播行为
   - 指定异常回滚策略

2. **事务隔离级别**:
   - 读操作采用默认隔离级别
   - 写操作使用可重复读或串行化隔离级别
   - 批量操作使用独立事务

### 3.4 异常处理

系统实现了全局统一的异常处理机制：

1. **GlobalExceptionHandler**:
   - 集中处理所有类型的异常
   - 将异常转换为规范的响应格式
   - 记录详细的错误日志

2. **异常分类**:
   - 参数校验异常
   - 业务逻辑异常
   - 系统运行时异常

3. **错误响应**:
   - 统一的错误码体系
   - 友好的错误消息
   - 必要的调试信息（仅在开发环境）

### 3.5 动态查询

系统实现了灵活的动态查询功能：

1. **QueryWrapper构建**:
   - 支持多种查询条件（等于、大于、小于、模糊匹配等）
   - 条件之间的逻辑组合
   - 动态排序

2. **参数解析**:
   - 特殊后缀解析（_gt, _lt, _like）
   - 时间范围查询（startTime, endTime）
   - 自动忽略空值参数

## 4. 使用教程

### 4.1 系统要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Redis 5.0+

### 4.2 环境配置

#### 4.2.1 数据库配置

在`application.yml`中配置MySQL连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/battery_monitor?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

#### 4.2.2 Redis配置

在`application.yml`中配置Redis连接：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 10000
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1
```

### 4.3 系统启动

#### 4.3.1 通过IDE启动

1. 导入项目到IDE（如IntelliJ IDEA）
2. 确保Maven依赖正确加载
3. 运行`XiangYuZhaoApplication.java`主类

#### 4.3.2 通过命令行启动

```bash
# 打包项目
mvn clean package -DskipTests

# 运行jar包
java -jar target/xiangyuzhao-0.0.1-SNAPSHOT.jar
```

### 4.4 API使用示例

#### 4.4.1 车辆管理

1. **查询车辆列表**

```bash
curl -X GET "http://localhost:8080/api/vehicle?page=1&size=10"
```

2. **添加车辆**

```bash
curl -X POST "http://localhost:8080/api/vehicle" \
  -H "Content-Type: application/json" \
  -d '[{"carId": 101, "vid": "VID001", "name": "测试车辆1"}]'
```

3. **更新车辆**

```bash
curl -X PUT "http://localhost:8080/api/vehicle" \
  -H "Content-Type: application/json" \
  -d '[{"id": 1, "carId": 101, "name": "测试车辆1-更新"}]'
```

4. **删除车辆**

```bash
curl -X DELETE "http://localhost:8080/api/vehicle?ids=1"
```

#### 4.4.2 电池信号上报与处理

1. **上报电池信号**

```bash
curl -X POST "http://localhost:8080/api/report" \
  -H "Content-Type: application/json" \
  -d '[{
    "carId": 101,
    "warnId": "BAT001",
    "signal": {
      "voltage": 12.5,
      "current": 10.2,
      "temperature": 35.6
    }
  }]'
```

2. **处理电池信号并返回预警**

```bash
curl -X POST "http://localhost:8080/api/warn" \
  -H "Content-Type: application/json" \
  -d '[{
    "carId": 101,
    "warnId": "BAT001",
    "signal": {
      "voltage": 12.5,
      "current": 10.2,
      "temperature": 35.6
    }
  }]'
```

3. **查询预警信息**

```bash
curl -X GET "http://localhost:8080/api/search?car_id=101&min_warn_time=2023-01-01%2000:00:00&max_warn_time=2023-12-31%2023:59:59"
```

#### 4.4.3 预警规则管理

1. **查询预警规则**

```bash
curl -X GET "http://localhost:8080/api/warnrule"
```

2. **添加预警规则**

```bash
curl -X POST "http://localhost:8080/api/warnrule" \
  -H "Content-Type: application/json" \
  -d '[{
    "name": "高温预警",
    "description": "电池温度超过40度",
    "condition": "$.temperature > 40",
    "level": 2,
    "enabled": true,
    "priority": 1
  }]'
```

### 4.5 常见操作流程

#### 4.5.1 车辆电池数据监控流程

1. **注册车辆**
   - 通过`/api/vehicle`接口创建车辆记录
   - 记录车辆的基本信息和唯一标识

2. **配置预警规则**
   - 通过`/api/warnrule`接口创建预警规则
   - 设置规则条件、预警级别和优先级

3. **上报电池信号**
   - 车辆定期通过`/api/report`接口上报电池信号
   - 系统存储信号数据用于后续分析

4. **预警处理**
   - 重要信号通过`/api/warn`接口进行实时处理
   - 系统根据预警规则判断是否生成预警
   - 预警信息被记录并可供查询

5. **预警查询与处理**
   - 通过`/api/search`接口查询车辆的预警信息
   - 通过`/api/warninfo`接口更新预警处理状态

#### 4.5.2 历史数据管理

1. **查询历史信号**
   - 通过`/api/batterysignal`接口查询历史信号数据
   - 支持基于车辆ID、时间范围等条件的查询

2. **数据清理**
   - 定期清理过期的历史数据
   - 可以通过服务层API进行自动化清理

### 4.6 最佳实践

#### 4.6.1 API调用建议

1. **批量操作**
   - 尽可能使用批量接口减少网络请求
   - 单次批量操作数量控制在100条以内

2. **查询优化**
   - 使用精确的查询条件减少返回数据量
   - 合理设置分页参数，避免一次查询过多数据

3. **错误处理**
   - 捕获并处理API返回的错误信息
   - 实施重试策略处理临时性错误

#### 4.6.2 系统维护

1. **日志监控**
   - 定期检查系统日志，关注错误和警告信息
   - 设置日志告警，及时发现系统异常

2. **性能优化**
   - 监控数据库查询性能，优化慢查询
   - 合理配置缓存策略，提高系统响应速度

3. **数据备份**
   - 定期备份关键数据，防止数据丢失
   - 制定数据恢复策略应对潜在风险

## 5. 附录

### 5.1 错误码对照表

| 错误码 | 描述                       |
|--------|--------------------------|
| 400    | 请求参数错误                |
| 401    | 未授权访问                  |
| 403    | 禁止访问                   |
| 404    | 资源不存在                  |
| 405    | 方法不允许                  |
| 500    | 服务器内部错误               |

### 5.2 常见问题解答

1. **Q: 系统支持的最大车辆数量是多少？**
   A: 系统设计上没有严格限制，实际上取决于服务器硬件配置和数据库性能。一般建议车辆数控制在10万辆以内以保证最佳性能。

2. **Q: 数据上报的频率有什么建议？**
   A: 根据电池监控需求，建议正常状况下5-15分钟上报一次，异常状况可适当提高频率。过高的频率会增加系统负载。

3. **Q: 如何处理历史数据积累问题？**
   A: 系统提供数据清理API，建议根据业务需求设置数据保留策略，如普通数据保留3个月，异常数据保留1年等。

4. **Q: 预警规则如何优化？**
   A: 建议定期分析预警触发情况，调整规则条件和优先级，避免过多的误报或漏报。

### 5.3 联系方式

如有问题或建议，请联系系统管理员。 