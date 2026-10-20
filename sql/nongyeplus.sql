/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80031
 Source Host           : localhost:3306
 Source Schema         : nongyeplus

 Target Server Type    : MySQL
 Target Server Version : 80031
 File Encoding         : 65001

 Date: 13/10/2026 19:16:27
*/

SET NAMES utf8mb4;
USE nongyeplus;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for complaint
-- ----------------------------
DROP TABLE IF EXISTS `complaint`;
CREATE TABLE `complaint`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NULL DEFAULT NULL COMMENT '关联批次',
  `complainant` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '投诉人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '投诉类型',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '投诉内容',
  `images` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '凭证图片（逗号分隔）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PENDING' COMMENT 'PENDING待处理/PROCESSING处理中/CLOSED已关闭',
  `reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '处理回复',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `reply_time` datetime NULL DEFAULT NULL,
  `consumer_reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消费者确认回复',
  `consumer_reply_time` datetime NULL DEFAULT NULL COMMENT '消费者确认时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_complaint_batch`(`batch_id` ASC) USING BTREE,
  INDEX `idx_complaint_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_complaint_batch` FOREIGN KEY (`batch_id`) REFERENCES `crop_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '投诉表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of complaint
-- ----------------------------
INSERT INTO `complaint` VALUES (1, 1, '王女士', '13600000001', '品质问题', '买的西红柿有烂果，希望核实', NULL, 'PENDING', NULL, '2026-10-13 09:02:59', NULL, NULL, NULL);

-- ----------------------------
-- Table structure for crop_batch
-- ----------------------------
DROP TABLE IF EXISTS `crop_batch`;
CREATE TABLE `crop_batch`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '批次编号',
  `farmer_id` bigint NOT NULL COMMENT '所属农户ID',
  `crop_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '作物名称',
  `variety` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品种',
  `land_location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '产地/地块位置',
  `plant_date` date NULL DEFAULT NULL COMMENT '播种日期',
  `expect_harvest_date` date NULL DEFAULT NULL COMMENT '预计采收日期',
  `actual_harvest_date` date NULL DEFAULT NULL COMMENT '实际采收日期',
  `harvest_weight` decimal(10, 2) NULL DEFAULT NULL COMMENT '采收重量（斤）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PLANTING' COMMENT 'PLANTING种植中/HARVESTED已采收/SOLD已售罄',
  `trace_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '溯源码',
  `qr_code_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '二维码图片路径',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '软删除：0正常 1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `batch_no`(`batch_no` ASC) USING BTREE,
  INDEX `idx_crop_batch_farmer`(`farmer_id` ASC) USING BTREE,
  INDEX `idx_crop_batch_status`(`status` ASC) USING BTREE,
  INDEX `idx_crop_batch_trace`(`trace_code` ASC) USING BTREE,
  INDEX `idx_crop_batch_crop`(`crop_name` ASC) USING BTREE,
  CONSTRAINT `fk_crop_batch_farmer` FOREIGN KEY (`farmer_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '种植批次表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of crop_batch
-- ----------------------------
INSERT INTO `crop_batch` VALUES (1, 'BATCH202603001', 2, '西红柿', '粉冠一号', '山东省寿光市洛城街道张家庄村3号大棚', '2026-03-01', '2026-06-15', '2026-06-14', 2000.00, 'HARVESTED', 'TZ202603001001', NULL, 0, '2026-10-13 09:02:59');
INSERT INTO `crop_batch` VALUES (2, 'BATCH202604002', 3, '黄瓜', '津优35号', '山东省寿光市孙家集街道李家庄', '2026-04-10', '2026-07-01', NULL, NULL, 'PLANTING', NULL, NULL, 0, '2026-10-13 09:02:59');

-- ----------------------------
-- Table structure for detection_report
-- ----------------------------
DROP TABLE IF EXISTS `detection_report`;
CREATE TABLE `detection_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `report_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '报告编号',
  `institution` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '检测机构',
  `report_url` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '报告文件路径',
  `report_date` date NULL DEFAULT NULL COMMENT '检测日期',
  `result` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '合格/不合格',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_detection_report_batch`(`batch_id` ASC) USING BTREE,
  CONSTRAINT `fk_detection_report_batch` FOREIGN KEY (`batch_id`) REFERENCES `crop_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '检测报告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of detection_report
-- ----------------------------
INSERT INTO `detection_report` VALUES (1, 1, 'TEST202606001', '寿光市农产品质量检测中心', NULL, '2026-06-10', '合格', '2026-10-13 09:02:59');

-- ----------------------------
-- Table structure for farming_log
-- ----------------------------
DROP TABLE IF EXISTS `farming_log`;
CREATE TABLE `farming_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL COMMENT '所属批次ID',
  `log_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SOWING播种/FERTILIZE施肥/PESTICIDE打药/IRRIGATE灌溉/WEED除草',
  `log_date` date NOT NULL COMMENT '操作日期',
  `material` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '使用物料（肥料/农药名）',
  `dosage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用量',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '操作描述',
  `photo_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '现场照片路径',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_farming_log_batch`(`batch_id` ASC) USING BTREE,
  INDEX `idx_farming_log_type`(`log_type` ASC) USING BTREE,
  CONSTRAINT `fk_farming_log_batch` FOREIGN KEY (`batch_id`) REFERENCES `crop_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '农事操作记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of farming_log
-- ----------------------------
INSERT INTO `farming_log` VALUES (1, 1, 'SOWING', '2026-03-01', '粉冠一号种子', '50g', '大棚育苗播种，棚内温度25度', NULL, '2026-10-13 09:02:59');
INSERT INTO `farming_log` VALUES (2, 1, 'FERTILIZE', '2026-03-20', '腐熟有机肥', '500kg/亩', '定植前施足底肥', NULL, '2026-10-13 09:02:59');
INSERT INTO `farming_log` VALUES (3, 1, 'PESTICIDE', '2026-04-15', '吡虫啉', '20g/亩', '防治蚜虫，安全间隔期7天', NULL, '2026-10-13 09:02:59');
INSERT INTO `farming_log` VALUES (4, 1, 'IRRIGATE', '2026-05-01', NULL, NULL, '滴灌浇水2小时', NULL, '2026-10-13 09:02:59');
INSERT INTO `farming_log` VALUES (5, 1, 'FERTILIZE', '2026-05-20', '复合肥N-P-K=15-15-15', '30kg/亩', '结果期追肥', NULL, '2026-10-13 09:02:59');
INSERT INTO `farming_log` VALUES (6, 2, 'SOWING', '2026-04-10', '津优35号黄瓜种子', '30g', '直播播种', NULL, '2026-10-13 09:02:59');

-- ----------------------------
-- Table structure for scan_log
-- ----------------------------
DROP TABLE IF EXISTS `scan_log`;
CREATE TABLE `scan_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NULL DEFAULT NULL,
  `trace_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `scan_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_scan_log_batch`(`batch_id` ASC) USING BTREE,
  INDEX `idx_scan_log_time`(`scan_time` ASC) USING BTREE,
  CONSTRAINT `fk_scan_log_batch` FOREIGN KEY (`batch_id`) REFERENCES `crop_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '扫码记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of scan_log
-- ----------------------------
INSERT INTO `scan_log` VALUES (1, 1, 'TZ202603001001', '0:0:0:0:0:0:0:1', 'curl/8.21.0', '2026-10-13 18:39:48');
INSERT INTO `scan_log` VALUES (2, 1, 'TZ202603001001', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.9131 SLBChan/128 SLBVPV/64-bit', '2026-10-13 18:49:10');
INSERT INTO `scan_log` VALUES (3, 1, 'TZ202603001001', '127.0.0.1', 'Python-urllib/3.7', '2026-10-13 18:55:53');
INSERT INTO `scan_log` VALUES (4, 1, 'TZ202603001001', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.9131 SLBChan/128 SLBVPV/64-bit', '2026-10-13 19:03:18');
INSERT INTO `scan_log` VALUES (5, 1, 'TZ202603001001', '192.168.31.184', 'Mozilla/5.0 (Linux; U; Android 16; zh-CN; V2505A Build/BP2A.250605.031.A3_V000L1) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/144.0.7559.86 Quark/10.15.6.1131 Mobile Safari/537.36', '2026-10-13 19:04:32');
INSERT INTO `scan_log` VALUES (6, 1, 'TZ202603001001', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 SLBrowser/9.0.8.9131 SLBChan/128 SLBVPV/64-bit', '2026-10-13 19:11:54');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（BCrypt 哈希，启动时由 MD5 迁移）',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名/企业名',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色：farmer农户/dealer经销商/admin管理员',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `avatar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像图片路径',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '软删除：0正常 1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  INDEX `idx_sys_user_role`(`role` ASC) USING BTREE,
  INDEX `idx_sys_user_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$nXC5CzDbHZ4AJbw1TutwWOyjLohVd5PMKJAKyMt77O9ySbcLzDpM.', '系统管理员', 'admin', '13800000000', '平台后台', '/uploads/avatar/364ba1dffaf542048647b1d79b92cb31.jpg', 1, 0, '2026-10-13 09:02:59');
INSERT INTO `sys_user` VALUES (2, 'farmer01', '$2a$10$JsE81fqQcUdG2mBqdWrCYOtjQj5YHcIBi2DPi.Sgg/SgF0zbsl3nu', '张建国', 'farmer', '13900000001', '山东省寿光市洛城街道', NULL, 1, 0, '2026-10-13 09:02:59');
INSERT INTO `sys_user` VALUES (3, 'farmer02', '$2a$10$fyQdn97xFRQHli0k/gM0LORWhe9OgQZ/O1K13LT0E4PQ.DT4izSwu', '李秀兰', 'farmer', '13900000002', '山东省寿光市孙家集街道', NULL, 1, 0, '2026-10-13 09:02:59');
INSERT INTO `sys_user` VALUES (4, 'dealer01', '$2a$10$OAFIa.fOkq.TZYbI6/Of7Ole/E1PfRPUx16n5qhSLtJbQvH3m9IgS', '绿丰果蔬批发', 'dealer', '13700000001', '寿光农产品物流园A区12号', NULL, 1, 0, '2026-10-13 09:02:59');
INSERT INTO `sys_user` VALUES (5, 'dealer02', '$2a$10$UcO5oYAQ/WXrYhF7pmDEA.DQtf/JJJ0LcCdGhY0ciz7FioFkJzk6S', '鲜达商超采购', 'dealer', '13700000002', '哈尔滨市哈达果蔬批发市场', NULL, 1, 0, '2026-10-13 09:02:59');
INSERT INTO `sys_user` VALUES (6, 'farmer03', '$2a$10$eolkhUbiz4RQgXbWQCYx3udSo0i4Qjw5l8RjMshZtzoExPxwfN8FS', '????', 'farmer', '13900000003', '????', NULL, 1, 0, '2026-10-13 13:30:14');
INSERT INTO `sys_user` VALUES (7, 'dealer03', '$2a$10$9JpQM3FB08ecFQ/eZmCk5OsMBVOQ8ZlWN/WHrHhgxsEGWCBEpiLce', '?????', 'dealer', '13700000003', '????', NULL, 1, 0, '2026-10-13 13:30:52');

-- ----------------------------
-- Table structure for trace_record
-- ----------------------------
DROP TABLE IF EXISTS `trace_record`;
CREATE TABLE `trace_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL COMMENT '批次ID',
  `from_user` bigint NULL DEFAULT NULL COMMENT '上家操作人ID',
  `to_user` bigint NULL DEFAULT NULL COMMENT '下家操作人ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'HARVEST采收/INBOUND入库/OUTBOUND出库/SALE销售',
  `quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '数量（斤）',
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作地点',
  `operate_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_trace_record_batch`(`batch_id` ASC) USING BTREE,
  INDEX `idx_trace_record_from`(`from_user` ASC) USING BTREE,
  INDEX `idx_trace_record_to`(`to_user` ASC) USING BTREE,
  INDEX `idx_trace_record_type`(`type` ASC) USING BTREE,
  CONSTRAINT `fk_trace_record_batch` FOREIGN KEY (`batch_id`) REFERENCES `crop_batch` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_trace_record_from` FOREIGN KEY (`from_user`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_trace_record_to` FOREIGN KEY (`to_user`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流转记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of trace_record
-- ----------------------------
INSERT INTO `trace_record` VALUES (1, 1, 2, NULL, 'HARVEST', 2000.00, '寿光市洛城街道张家庄村3号大棚', '2026-10-13 09:02:59', '采收西红柿2000斤');
INSERT INTO `trace_record` VALUES (2, 1, 2, 4, 'INBOUND', 2000.00, '寿光农产品物流园A区12号', '2026-10-13 09:02:59', '张建国卖给绿丰果蔬批发');
INSERT INTO `trace_record` VALUES (3, 1, 4, 5, 'OUTBOUND', 1500.00, '寿光物流园→哈尔滨哈达市场', '2026-10-13 09:02:59', '绿丰批发发往哈尔滨鲜达商超');

SET FOREIGN_KEY_CHECKS = 1;
