# 小米汽车电池预警系统

小米汽车电池预警系统是一个专门用于监控车辆电池信号并生成预警的服务系统。系统可以处理高并发请求和大规模数据，支持不同电池类型的规则配置。

## 功能特性

- 支持车辆电池信号上报和处理
- 支持多种电池类型（三元电池/铁锂电池）的不同预警规则
- 使用Redis缓存提高系统性能
- 使用RabbitMQ消息队列处理异步预警
- 规则引擎支持动态规则解析
- 支持百万/千万级数据处理

## 系统架构

系统采用MVC架构，主要包含以下组件：

- 实体层：Vehicle、WarnRule、BatterySignal、WarnInfo
- DAO层：各实体对应的Mapper接口
- 服务层：VehicleService、WarnRuleService、BatterySignalService、WarnService、RuleEngineService等
- 控制层：BatteryWarnController
- 缓存层：CacheService
- 配置类：RedisConfig、RabbitMQConfig等

## 技术栈

- SpringBoot 2.6.7
- MyBatis 2.2.2
- Redis
- RabbitMQ
- MySQL
- Lombok

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Redis 5.0+
- RabbitMQ 3.8+

### 数据库准备

创建数据库：

```sql
CREATE DATABASE battery_warn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

初始化表结构（SQL脚本位于`src/main/resources/sql`目录下）。

### 配置修改

根据实际环境修改`application.yml`配置文件：

- 数据库连接信息
- Redis连接信息
- RabbitMQ连接信息

### 项目构建

```bash
mvn clean package
```

### 启动服务

```bash
java -jar target/battery-warn-system-1.0.0.jar
```

## API接口

### 信号上报接口

```
POST /api/warn
```

请求示例：

```json
[
  {
    "carId": 1,
    "signal": "{\"Mx\":4.2,\"Mi\":3.8,\"Ix\":10.5,\"Ii\":9.8}",
    "warnId": 1
  }
]
```

### 查询预警接口

```
GET /api/warn/{carId}
```

## 预警规则格式

规则JSON示例：

```json
{
  "expression": "maxVoltage - minVoltage",
  "thresholds": [
    {
      "level": 0,
      "operator": ">=",
      "value": 0.5
    },
    {
      "level": 1,
      "operator": ">=",
      "value": 0.4
    },
    {
      "level": 2,
      "operator": ">=",
      "value": 0.3
    },
    {
      "level": 3,
      "operator": ">=",
      "value": 0.2
    },
    {
      "level": 4,
      "operator": ">=",
      "value": 0.1
    }
  ]
}
```

## 开发者

- XiangYuZhao 