# 电池监控系统 API 使用文档

## 目录

1. [车辆管理 API](#1-车辆管理-api)
2. [电池信号管理 API](#2-电池信号管理-api)
3. [预警规则管理 API](#3-预警规则管理-api)
4. [预警信息管理 API](#4-预警信息管理-api)
5. [报告与预警专用 API](#5-报告与预警专用-api)

## 1. 车辆管理 API

### 1.1 查询车辆

**请求方式:** `GET`

**请求路径:** `/api/vehicle`

**功能说明:** 查询车辆信息，支持分页和动态过滤条件

**请求参数:**

| 参数名 | 类型 | 必填 | 说明 |
| ------ | ---- | ---- | ---- |
| page | Long | 否 | 页码，默认为1 |
| size | Long | 否 | 每页大小，默认为10 |
| carId | Integer | 否 | 车辆ID，精确匹配 |
| vid | String | 否 | 车辆唯一标识，精确匹配 |
| name | String | 否 | 车辆名称，精确匹配 |
| name_like | String | 否 | 车辆名称，模糊匹配 |
| status | Integer | 否 | 车辆状态，精确匹配 |
| createTime_gt | Date | 否 | 创建时间大于，格式为yyyy-MM-dd HH:mm:ss |
| createTime_lt | Date | 否 | 创建时间小于，格式为yyyy-MM-dd HH:mm:ss |
| startTime | Date | 否 | 起始时间，与endTime一起使用，格式为yyyy-MM-dd HH:mm:ss |
| endTime | Date | 否 | 结束时间，与startTime一起使用，格式为yyyy-MM-dd HH:mm:ss |

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "carId": 101,
        "vid": "VID0123456789ABCD",
        "name": "测试车辆1",
        "status": 1,
        "createTime": "2023-01-01 12:00:00",
        "updateTime": "2023-01-02 12:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "orders": [],
    "optimizeCountSql": true,
    "searchCount": true,
    "pages": 1
  }
}
```

### 1.2 新增车辆

**请求方式:** `POST`

**请求路径:** `/api/vehicle`

**功能说明:** 新增一个或多个车辆信息

**请求参数:**

```json
[
  {
    "carId": 101,
    "vid": "VID0123456789ABCD",
    "name": "测试车辆1",
    "status": 1
  }
]
```

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "carId": 101,
      "vid": "VID0123456789ABCD",
      "name": "测试车辆1",
      "status": 1,
      "createTime": "2023-01-01 12:00:00",
      "updateTime": "2023-01-01 12:00:00"
    }
  ]
}
```

### 1.3 更新车辆

**请求方式:** `PUT`

**请求路径:** `/api/vehicle`

**功能说明:** 更新一个或多个车辆信息

**请求参数:**

```json
[
  {
    "id": 1,
    "carId": 101,
    "vid": "VID0123456789ABCD",
    "name": "测试车辆1-更新",
    "status": 2
  }
]
```

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

### 1.4 删除车辆

**请求方式:** `DELETE`

**请求路径:** `/api/vehicle`

**功能说明:** 删除一个或多个车辆信息

**请求参数:**

| 参数名 | 类型 | 必填 | 说明 |
| ------ | ---- | ---- | ---- |
| ids | String | 是 | 车辆ID列表，多个ID使用逗号分隔，如"1,2,3" |

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

## 2. 电池信号管理 API

### 2.1 查询电池信号

**请求方式:** `GET`

**请求路径:** `/api/batterysignal`

**功能说明:** 查询电池信号信息，支持分页和动态过滤条件

**请求参数:**

| 参数名 | 类型 | 必填 | 说明 |
| ------ | ---- | ---- | ---- |
| page | Long | 否 | 页码，默认为1 |
| size | Long | 否 | 每页大小，默认为10 |
| carId | Integer | 否 | 车辆ID，精确匹配 |
| warnId | Integer | 否 | 预警ID，精确匹配 |
| processed | Boolean | 否 | 是否已处理，true或false |
| signalTime_gt | Date | 否 | 信号时间大于，格式为yyyy-MM-dd HH:mm:ss |
| signalTime_lt | Date | 否 | 信号时间小于，格式为yyyy-MM-dd HH:mm:ss |
| startTime | Date | 否 | 起始时间，与endTime一起使用，格式为yyyy-MM-dd HH:mm:ss |
| endTime | Date | 否 | 结束时间，与startTime一起使用，格式为yyyy-MM-dd HH:mm:ss |

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "carId": 101,
        "warnId": 1,
        "signalData": "{\"voltage\":12.5,\"current\":10.2,\"temperature\":35.6}",
        "signalTime": "2023-01-01 12:00:00",
        "processed": true,
        "processTime": "2023-01-01 12:01:00",
        "createTime": "2023-01-01 12:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "orders": [],
    "optimizeCountSql": true,
    "searchCount": true,
    "pages": 1
  }
}
```

### 2.2 新增电池信号

**请求方式:** `POST`

**请求路径:** `/api/batterysignal`

**功能说明:** 新增一个或多个电池信号记录

**请求参数:**

```json
[
  {
    "carId": 101,
    "warnId": 1,
    "signalData": "{\"voltage\":12.5,\"current\":10.2,\"temperature\":35.6}",
    "signalTime": "2023-01-01 12:00:00"
  }
]
```

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "carId": 101,
      "warnId": 1,
      "signalData": "{\"voltage\":12.5,\"current\":10.2,\"temperature\":35.6}",
      "signalTime": "2023-01-01 12:00:00",
      "processed": false,
      "processTime": null,
      "createTime": "2023-01-01 12:00:00"
    }
  ]
}
```

### 2.3 更新电池信号

**请求方式:** `PUT`

**请求路径:** `/api/batterysignal`

**功能说明:** 更新一个或多个电池信号记录

**请求参数:**

```json
[
  {
    "id": 1,
    "carId": 101,
    "warnId": 1,
    "signalData": "{\"voltage\":12.6,\"current\":10.3,\"temperature\":35.7}",
    "signalTime": "2023-01-01 12:00:00",
    "processed": true
  }
]
```

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

### 2.4 删除电池信号

**请求方式:** `DELETE`

**请求路径:** `/api/batterysignal`

**功能说明:** 删除一个或多个电池信号记录

**请求参数:**

| 参数名 | 类型 | 必填 | 说明 |
| ------ | ---- | ---- | ---- |
| ids | String | 是 | 电池信号ID列表，多个ID使用逗号分隔，如"1,2,3" |

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

## 3. 预警规则管理 API

### 3.1 查询预警规则

**请求方式:** `GET`

**请求路径:** `/api/warnrule`

**功能说明:** 查询预警规则信息，支持分页和动态过滤条件

**请求参数:**

| 参数名 | 类型 | 必填 | 说明 |
| ------ | ---- | ---- | ---- |
| page | Long | 否 | 页码，默认为1 |
| size | Long | 否 | 每页大小，默认为10 |
| name | String | 否 | 规则名称，精确匹配 |
| name_like | String | 否 | 规则名称，模糊匹配 |
| level | Integer | 否 | 预警级别，精确匹配 |
| enabled | Boolean | 否 | 是否启用，true或false |
| priority_gt | Integer | 否 | 优先级大于 |
| priority_lt | Integer | 否 | 优先级小于 |

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "高温预警",
        "description": "电池温度超过40度",
        "condition": "$.temperature > 40",
        "level": 2,
        "enabled": true,
        "priority": 1,
        "createTime": "2023-01-01 12:00:00",
        "updateTime": "2023-01-01 12:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "orders": [],
    "optimizeCountSql": true,
    "searchCount": true,
    "pages": 1
  }
}
```

### 3.2 新增预警规则

**请求方式:** `POST`

**请求路径:** `/api/warnrule`

**功能说明:** 新增一个或多个预警规则

**请求参数:**

```json
[
  {
    "name": "高温预警",
    "description": "电池温度超过40度",
    "condition": "$.temperature > 40",
    "level": 2,
    "enabled": true,
    "priority": 1
  }
]
```

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "高温预警",
      "description": "电池温度超过40度",
      "condition": "$.temperature > 40",
      "level": 2,
      "enabled": true,
      "priority": 1,
      "createTime": "2023-01-01 12:00:00",
      "updateTime": "2023-01-01 12:00:00"
    }
  ]
}
```

### 3.3 更新预警规则

**请求方式:** `PUT`

**请求路径:** `/api/warnrule`

**功能说明:** 更新一个或多个预警规则

**请求参数:**

```json
[
  {
    "id": 1,
    "name": "高温预警",
    "description": "电池温度超过45度",
    "condition": "$.temperature > 45",
    "level": 3,
    "enabled": true,
    "priority": 2
  }
]
```

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

### 3.4 删除预警规则

**请求方式:** `DELETE`

**请求路径:** `/api/warnrule`

**功能说明:** 删除一个或多个预警规则

**请求参数:**

| 参数名 | 类型 | 必填 | 说明 |
| ------ | ---- | ---- | ---- |
| ids | String | 是 | 预警规则ID列表，多个ID使用逗号分隔，如"1,2,3" |

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

## 4. 预警信息管理 API

### 4.1 查询预警信息

**请求方式:** `GET`

**请求路径:** `/api/warninfo`

**功能说明:** 查询预警信息，支持分页和动态过滤条件

**请求参数:**

| 参数名 | 类型 | 必填 | 说明 |
| ------ | ---- | ---- | ---- |
| page | Long | 否 | 页码，默认为1 |
| size | Long | 否 | 每页大小，默认为10 |
| carId | Integer | 否 | 车辆ID，精确匹配 |
| warnId | Integer | 否 | 预警ID，精确匹配 |
| signalId | Long | 否 | 信号ID，精确匹配 |
| warnLevel | Integer | 否 | 预警级别，精确匹配 |
| handled | Boolean | 否 | 是否已处理，true或false |
| warnTime_gt | Date | 否 | 预警时间大于，格式为yyyy-MM-dd HH:mm:ss |
| warnTime_lt | Date | 否 | 预警时间小于，格式为yyyy-MM-dd HH:mm:ss |
| startTime | Date | 否 | 起始时间，与endTime一起使用，格式为yyyy-MM-dd HH:mm:ss |
| endTime | Date | 否 | 结束时间，与startTime一起使用，格式为yyyy-MM-dd HH:mm:ss |

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "carId": 101,
        "warnId": 1,
        "signalId": 1,
        "warnTime": "2023-01-01 12:00:00",
        "warnLevel": 2,
        "warnDesc": "电池温度异常",
        "handled": false,
        "handleTime": null,
        "createTime": "2023-01-01 12:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "orders": [],
    "optimizeCountSql": true,
    "searchCount": true,
    "pages": 1
  }
}
```

### 4.2 新增预警信息

**请求方式:** `POST`

**请求路径:** `/api/warninfo`

**功能说明:** 新增一个或多个预警信息

**请求参数:**

```json
[
  {
    "carId": 101,
    "warnId": 1,
    "signalId": 1,
    "warnTime": "2023-01-01 12:00:00",
    "warnLevel": 2,
    "warnDesc": "电池温度异常"
  }
]
```

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "carId": 101,
      "warnId": 1,
      "signalId": 1,
      "warnTime": "2023-01-01 12:00:00",
      "warnLevel": 2,
      "warnDesc": "电池温度异常",
      "handled": false,
      "handleTime": null,
      "createTime": "2023-01-01 12:00:00"
    }
  ]
}
```

### 4.3 更新预警信息

**请求方式:** `PUT`

**请求路径:** `/api/warninfo`

**功能说明:** 更新一个或多个预警信息

**请求参数:**

```json
[
  {
    "id": 1,
    "carId": 101,
    "warnId": 1,
    "signalId": 1,
    "warnTime": "2023-01-01 12:00:00",
    "warnLevel": 2,
    "warnDesc": "电池温度异常",
    "handled": true,
    "handleTime": "2023-01-01 12:30:00"
  }
]
```

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

### 4.4 删除预警信息

**请求方式:** `DELETE`

**请求路径:** `/api/warninfo`

**功能说明:** 删除一个或多个预警信息

**请求参数:**

| 参数名 | 类型 | 必填 | 说明 |
| ------ | ---- | ---- | ---- |
| ids | String | 是 | 预警信息ID列表，多个ID使用逗号分隔，如"1,2,3" |

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

## 5. 报告与预警专用 API

### 5.1 处理电池信号并返回预警

**请求方式:** `POST`

**请求路径:** `/api/warn`

**功能说明:** 上报电池信号并根据预警规则生成预警信息

**请求参数:**

```json
[
  {
    "carId": 101,
    "warnId": 1,
    "signal": {
      "voltage": 12.5,
      "current": 10.2,
      "temperature": 45.6
    },
    "vid": "VID0123456789ABCD",
    "signalTime": "2023-01-01 12:00:00"
  }
]
```

**请求字段说明:**

| 字段名 | 类型 | 必填 | 说明 |
| ------ | ---- | ---- | ---- |
| carId | Integer | 是 | 车辆ID |
| warnId | Integer | 是 | 预警编号，如1表示电压差检测，2表示电流差检测 |
| signal | Object | 是 | 信号数据，包含电池相关指标 |
| vid | String | 否 | 车辆VID，格式为16位大写字母和数字，用于双重验证 |
| signalTime | Date | 否 | 信号时间，格式为yyyy-MM-dd HH:mm:ss，不提供则使用系统当前时间 |

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "signal": {
        "id": 1,
        "carId": 101,
        "warnId": 1,
        "signalData": "{\"voltage\":12.5,\"current\":10.2,\"temperature\":45.6}",
        "signalTime": "2023-01-01 12:00:00",
        "processed": true,
        "processTime": "2023-01-01 12:00:05",
        "createTime": "2023-01-01 12:00:00"
      },
      "parsedSignal": {
        "voltage": 12.5,
        "current": 10.2,
        "temperature": 45.6
      },
      "warnings": [
        {
          "id": 1,
          "carId": 101,
          "warnId": 1,
          "signalId": 1,
          "warnTime": "2023-01-01 12:00:05",
          "warnLevel": 2,
          "warnDesc": "电池温度异常: 45.6℃",
          "handled": false,
          "handleTime": null,
          "createTime": "2023-01-01 12:00:05"
        }
      ]
    }
  ]
}
```

### 5.2 上报电池信号

**请求方式:** `POST`

**请求路径:** `/api/report`

**功能说明:** 批量上报电池信号信息，不触发预警处理

**请求参数:**

```json
[
  {
    "carId": 101,
    "warnId": 1,
    "signal": {
      "voltage": 12.5,
      "current": 10.2,
      "temperature": 35.6
    },
    "vid": "VID0123456789ABCD",
    "signalTime": "2023-01-01 12:00:00"
  }
]
```

**请求字段说明:**

| 字段名 | 类型 | 必填 | 说明 |
| ------ | ---- | ---- | ---- |
| carId | Integer | 是 | 车辆ID |
| warnId | Integer | 是 | 预警编号，如1表示电压差检测，2表示电流差检测 |
| signal | Object | 是 | 信号数据，包含电池相关指标 |
| vid | String | 否 | 车辆VID，格式为16位大写字母和数字，用于双重验证 |
| signalTime | Date | 否 | 信号时间，格式为yyyy-MM-dd HH:mm:ss，不提供则使用系统当前时间 |

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "carId": 101,
      "warnId": 1,
      "signalData": "{\"voltage\":12.5,\"current\":10.2,\"temperature\":35.6}",
      "signalTime": "2023-01-01 12:00:00",
      "processed": false,
      "processTime": null,
      "createTime": "2023-01-01 12:00:00"
    }
  ]
}
```

### 5.3 查询预警信息

**请求方式:** `GET`

**请求路径:** `/api/search`

**功能说明:** 根据条件查询车辆的预警信息

**请求参数:**

| 参数名 | 类型 | 必填 | 说明 |
| ------ | ---- | ---- | ---- |
| car_id | Integer | 是 | 车辆ID |
| warn_id | Integer | 否 | 预警ID |
| min_warn_time | Date | 否 | 最小预警时间，格式为yyyy-MM-dd HH:mm:ss |
| max_warn_time | Date | 否 | 最大预警时间，格式为yyyy-MM-dd HH:mm:ss |
| handled | Boolean | 否 | 是否已处理，true或false |

**响应参数:**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "carId": 101,
      "warnId": 1,
      "signalId": 1,
      "warnTime": "2023-01-01 12:00:00",
      "warnLevel": 2,
      "warnDesc": "电池温度异常",
      "handled": false,
      "handleTime": null,
      "createTime": "2023-01-01 12:00:00"
    }
  ]
}
```

## 错误码说明

| 错误码 | 说明 |
| ------ | ---- |
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 405 | 请求方法不允许 |
| 500 | 服务器内部错误 | 