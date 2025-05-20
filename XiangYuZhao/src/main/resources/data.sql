USE xiaomi_car_battery;

-- 插入车辆信息
INSERT INTO vehicle (vid, battery_type, mileage, battery_health) VALUES
('VID1234567890123', '三元电池', 100.00, 100.00),
('VID2345678901234', '铁锂电池', 600.00, 95.00),
('VID3456789012345', '三元电池', 300.00, 98.00);

-- 三元电池电压差报警规则
INSERT INTO warn_rule (warn_id, warn_name, battery_type, rule) VALUES
(1, '电压差报警', '三元电池', '{
  "leftOperand": "Mx",
  "rightOperand": "Mi",
  "operator": 1,
  "rules": [
    {"minValue": 5.0, "maxValue": null, "includeMin": true, "includeMax": false, "level": 0},
    {"minValue": 3.0, "maxValue": 5.0, "includeMin": true, "includeMax": false, "level": 1},
    {"minValue": 1.0, "maxValue": 3.0, "includeMin": true, "includeMax": false, "level": 2},
    {"minValue": 0.6, "maxValue": 1.0, "includeMin": true, "includeMax": false, "level": 3},
    {"minValue": 0.2, "maxValue": 0.6, "includeMin": true, "includeMax": false, "level": 4}
  ]
}');

-- 铁锂电池电压差报警规则
INSERT INTO warn_rule (warn_id, warn_name, battery_type, rule) VALUES
(1, '电压差报警', '铁锂电池', '{
  "leftOperand": "Mx",
  "rightOperand": "Mi",
  "operator": 1,
  "rules": [
    {"minValue": 2.0, "maxValue": null, "includeMin": true, "includeMax": false, "level": 0},
    {"minValue": 1.0, "maxValue": 2.0, "includeMin": true, "includeMax": false, "level": 1},
    {"minValue": 0.7, "maxValue": 1.0, "includeMin": true, "includeMax": false, "level": 2},
    {"minValue": 0.4, "maxValue": 0.7, "includeMin": true, "includeMax": false, "level": 3},
    {"minValue": 0.2, "maxValue": 0.4, "includeMin": true, "includeMax": false, "level": 4}
  ]
}');

-- 三元电池电流差报警规则
INSERT INTO warn_rule (warn_id, warn_name, battery_type, rule) VALUES
(2, '电流差报警', '三元电池', '{
  "leftOperand": "Ix",
  "rightOperand": "Ii",
  "operator": 1,
  "rules": [
    {"minValue": 3.0, "maxValue": null, "includeMin": true, "includeMax": false, "level": 0},
    {"minValue": 1.0, "maxValue": 3.0, "includeMin": true, "includeMax": false, "level": 1},
    {"minValue": 0.2, "maxValue": 1.0, "includeMin": true, "includeMax": false, "level": 2}
  ]
}');

-- 铁锂电池电流差报警规则
INSERT INTO warn_rule (warn_id, warn_name, battery_type, rule) VALUES
(2, '电流差报警', '铁锂电池', '{
  "leftOperand": "Ix",
  "rightOperand": "Ii",
  "operator": 1,
  "rules": [
    {"minValue": 1.0, "maxValue": null, "includeMin": true, "includeMax": false, "level": 0},
    {"minValue": 0.5, "maxValue": 1.0, "includeMin": true, "includeMax": false, "level": 1},
    {"minValue": 0.2, "maxValue": 0.5, "includeMin": true, "includeMax": false, "level": 2}
  ]
}');

-- 插入电池信号数据（增加processed和process_time字段）
INSERT INTO battery_signal (car_id, warn_id, signal_data, signal_time, processed, process_time) VALUES
(1, 1, '{"Mx":12.5,"Mi":0.5,"Ix":10.0,"Ii":9.5}', '2023-05-01 08:30:00', 1, '2023-05-01 08:31:00'),
(1, 2, '{"Mx":12.2,"Mi":0.6,"Ix":9.8,"Ii":9.2}', '2023-05-01 09:15:00', 1, '2023-05-01 09:16:00'),
(2, 1, '{"Mx":11.8,"Mi":0.7,"Ix":9.5,"Ii":9.0}', '2023-05-01 10:00:00', 1, '2023-05-01 10:01:00'),
(2, 2, '{"Mx":11.5,"Mi":0.8,"Ix":9.2,"Ii":8.8}', '2023-05-01 10:45:00', 1, '2023-05-01 10:46:00'),
(3, 1, '{"Mx":11.2,"Mi":0.9,"Ix":8.9,"Ii":8.5}', '2023-05-01 11:30:00', 1, '2023-05-01 11:31:00'),
-- 新增一些未处理的信号用于测试
(1, 1, '{"Mx":12.6,"Mi":0.4,"Ix":10.2,"Ii":9.4}', '2023-05-02 08:30:00', 0, NULL),
(2, 2, '{"Mx":11.6,"Mi":0.7,"Ix":9.3,"Ii":8.7}', '2023-05-02 10:45:00', 0, NULL),
(3, 1, '{"Mx":11.3,"Mi":0.8,"Ix":9.0,"Ii":8.4}', '2023-05-02 11:30:00', 0, NULL);

-- 插入预警信息（包含warn_time和signal_time字段）
INSERT INTO warn_info (car_id, warn_id, warn_name, warn_level, signal_id, warn_time, signal_time) VALUES
(1, 1, '电压差报警', 0, 1, '2023-05-01 08:31:00', '2023-05-01 08:30:00'),
(1, 2, '电流差报警', 1, 2, '2023-05-01 09:16:00', '2023-05-01 09:15:00'),
(2, 1, '电压差报警', 0, 3, '2023-05-01 10:01:00', '2023-05-01 10:00:00'),
(2, 2, '电流差报警', 1, 4, '2023-05-01 10:46:00', '2023-05-01 10:45:00'),
(3, 1, '电压差报警', 0, 5, '2023-05-01 11:31:00', '2023-05-01 11:30:00'); 