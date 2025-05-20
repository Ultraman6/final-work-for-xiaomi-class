-- 创建数据库
CREATE DATABASE IF NOT EXISTS xiaomi_car_battery DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE xiaomi_car_battery;

-- 车辆信息表
CREATE TABLE IF NOT EXISTS `vehicle` (
  `car_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '车架编号',
  `vid` varchar(16) NOT NULL COMMENT '车辆识别码,16位随机字符串',
  `battery_type` varchar(20) NOT NULL COMMENT '电池类型:三元电池/铁锂电池',
  `mileage` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总里程(km)',
  `battery_health` decimal(5,2) NOT NULL DEFAULT '100.00' COMMENT '电池健康状态(%)',
  PRIMARY KEY (`car_id`),
  UNIQUE KEY `idx_vid` (`vid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆信息表';

-- 预警规则表
CREATE TABLE IF NOT EXISTS `warn_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `warn_id` int(11) NOT NULL COMMENT '预警编号',
  `warn_name` varchar(50) NOT NULL COMMENT '预警名称',
  `battery_type` varchar(20) NOT NULL COMMENT '电池类型',
  `rule` text NOT NULL COMMENT '预警规则JSON',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_rule_battery` (`warn_id`,`battery_type`) COMMENT '规则ID和电池类型唯一',
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则表';

-- 电池信号表
CREATE TABLE IF NOT EXISTS `battery_signal` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `warn_id` int(11) NOT NULL COMMENT '预警规则ID',
  `car_id` int(11) NOT NULL COMMENT '车架编号',
  `signal_data` json NOT NULL COMMENT '信号数据JSON',
  `signal_time` datetime NOT NULL COMMENT '信号时间',
  `process_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '处理状态:0-未处理,1-处理中,2-已处理,3-处理失败',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `retry_count` int(11) NOT NULL DEFAULT 0 COMMENT '重试次数',
  `version` int(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_car_warn_time` (`car_id`, `warn_id`, `signal_time`) COMMENT '车辆ID、预警规则ID和信号时间唯一',
  KEY `idx_car_id` (`car_id`),
  KEY `idx_process_status` (`process_status`) COMMENT '处理状态索引，优化未处理信号查询',
  KEY `idx_signal_time` (`signal_time`) COMMENT '信号时间索引，用于分区查询',
  CONSTRAINT `fk_battery_signal_car_id` FOREIGN KEY (`car_id`) REFERENCES `vehicle` (`car_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT="电池信号表";

-- 预警信息表
CREATE TABLE IF NOT EXISTS `warn_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `car_id` int(11) NOT NULL COMMENT '车架编号',
  `warn_id` int(11) NOT NULL COMMENT '触发的规则ID',
  `signal_id` bigint(20) NOT NULL COMMENT '关联的信号ID',
  `warn_name` varchar(50) NOT NULL COMMENT '预警规则名称',
  `warn_level` int(11) NOT NULL COMMENT '预警等级:0-4,0级最高',
  `warn_time` datetime NOT NULL COMMENT '预警完成时间',
  `signal_time` datetime NOT NULL COMMENT '关联的信号时间',
  PRIMARY KEY (`id`),
  KEY `idx_car_id` (`car_id`),
  KEY `idx_warn_level` (`warn_level`),
  KEY `idx_signal_id` (`signal_id`),
  KEY `idx_signal_time` (`signal_time`) COMMENT '信号时间索引，用于时间范围查询',
  CONSTRAINT `fk_warn_info_car_id` FOREIGN KEY (`car_id`) REFERENCES `vehicle` (`car_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_warn_info_signal_id` FOREIGN KEY (`signal_id`) REFERENCES `battery_signal` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警信息表';