-- =============================================
-- 农产品全链路溯源管理平台 - 数据库建表脚本
-- 数据库：nongyeplus
-- 说明：
--   1. 密码初始为 MD5（测试数据），应用启动时 PasswordMigrationRunner 会自动迁移为 BCrypt
--   2. 关键查询字段已加索引；表间外键保证引用完整性
--   3. sys_user / crop_batch 支持软删除（deleted 字段），应用层查询需过滤 deleted=0
-- =============================================

SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS nongyeplus DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE nongyeplus;

-- ----------------------------
-- 1. 用户表（农户/经销商/管理员）
-- ----------------------------
DROP TABLE IF EXISTS complaint;
DROP TABLE IF EXISTS detection_report;
DROP TABLE IF EXISTS trace_record;
DROP TABLE IF EXISTS farming_log;
DROP TABLE IF EXISTS crop_batch;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 哈希，启动时由 MD5 迁移）',
    real_name   VARCHAR(50)  COMMENT '真实姓名/企业名',
    role        VARCHAR(20)  NOT NULL COMMENT '角色：farmer农户/dealer经销商/admin管理员',
    phone       VARCHAR(20)  COMMENT '联系电话',
    address     VARCHAR(200) COMMENT '地址',
    avatar      VARCHAR(200) COMMENT '头像图片路径（/uploads/avatar/xxx.jpg），为空显示姓名首字',
    status      TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    deleted     TINYINT DEFAULT 0 COMMENT '软删除：0正常 1已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sys_user_role (role),
    INDEX idx_sys_user_phone (phone)
) COMMENT '系统用户表';

-- ----------------------------
-- 2. 种植批次表
-- ----------------------------
CREATE TABLE crop_batch (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_no            VARCHAR(50)  NOT NULL UNIQUE COMMENT '批次编号',
    farmer_id           BIGINT       NOT NULL COMMENT '所属农户ID',
    crop_name           VARCHAR(50)  NOT NULL COMMENT '作物名称',
    variety             VARCHAR(50)  COMMENT '品种',
    land_location       VARCHAR(200) COMMENT '产地/地块位置',
    plant_date          DATE         COMMENT '播种日期',
    expect_harvest_date DATE         COMMENT '预计采收日期',
    actual_harvest_date DATE         COMMENT '实际采收日期',
    harvest_weight      DECIMAL(10,2) COMMENT '采收重量（斤）',
    status              VARCHAR(20) DEFAULT 'PLANTING' COMMENT 'PLANTING种植中/HARVESTED已采收/SOLD已售罄',
    trace_code          VARCHAR(100) COMMENT '溯源码',
    qr_code_path        VARCHAR(200) COMMENT '二维码图片路径',
    deleted             TINYINT DEFAULT 0 COMMENT '软删除：0正常 1已删除',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_crop_batch_farmer (farmer_id),
    INDEX idx_crop_batch_status (status),
    INDEX idx_crop_batch_trace (trace_code),
    INDEX idx_crop_batch_crop (crop_name),
    CONSTRAINT fk_crop_batch_farmer FOREIGN KEY (farmer_id) REFERENCES sys_user(id)
) COMMENT '种植批次表';

-- ----------------------------
-- 3. 农事记录表
-- ----------------------------
CREATE TABLE farming_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id    BIGINT       NOT NULL COMMENT '所属批次ID',
    log_type    VARCHAR(20)  NOT NULL COMMENT '类型：SOWING播种/FERTILIZE施肥/PESTICIDE打药/IRRIGATE灌溉/WEED除草',
    log_date    DATE         NOT NULL COMMENT '操作日期',
    material    VARCHAR(100) COMMENT '使用物料（肥料/农药名）',
    dosage      VARCHAR(50)  COMMENT '用量',
    content     TEXT         COMMENT '操作描述',
    photo_path  VARCHAR(200) COMMENT '现场照片路径',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_farming_log_batch (batch_id),
    INDEX idx_farming_log_type (log_type),
    CONSTRAINT fk_farming_log_batch FOREIGN KEY (batch_id) REFERENCES crop_batch(id)
) COMMENT '农事操作记录表';

-- ----------------------------
-- 4. 流转记录表（从农户到经销商到终端）
-- ----------------------------
CREATE TABLE trace_record (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id    BIGINT       NOT NULL COMMENT '批次ID',
    from_user   BIGINT       COMMENT '上家操作人ID',
    to_user     BIGINT       COMMENT '下家操作人ID',
    type        VARCHAR(20)  NOT NULL COMMENT 'HARVEST采收/INBOUND入库/OUTBOUND出库/SALE销售',
    quantity    DECIMAL(10,2) COMMENT '数量（斤）',
    location    VARCHAR(200) COMMENT '操作地点',
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    remark      VARCHAR(200) COMMENT '备注',
    INDEX idx_trace_record_batch (batch_id),
    INDEX idx_trace_record_from (from_user),
    INDEX idx_trace_record_to (to_user),
    INDEX idx_trace_record_type (type),
    CONSTRAINT fk_trace_record_batch FOREIGN KEY (batch_id) REFERENCES crop_batch(id),
    CONSTRAINT fk_trace_record_from FOREIGN KEY (from_user) REFERENCES sys_user(id),
    CONSTRAINT fk_trace_record_to FOREIGN KEY (to_user) REFERENCES sys_user(id)
) COMMENT '流转记录表';

-- ----------------------------
-- 5. 检测报告表
-- ----------------------------
CREATE TABLE detection_report (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id    BIGINT       NOT NULL,
    report_no   VARCHAR(50)  COMMENT '报告编号',
    institution VARCHAR(100) COMMENT '检测机构',
    report_url  VARCHAR(200) COMMENT '报告文件路径',
    report_date  DATE         COMMENT '检测日期',
    result      VARCHAR(20)  COMMENT '合格/不合格',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_detection_report_batch (batch_id),
    CONSTRAINT fk_detection_report_batch FOREIGN KEY (batch_id) REFERENCES crop_batch(id)
) COMMENT '检测报告表';

-- ----------------------------
-- 6. 投诉表
-- ----------------------------
CREATE TABLE complaint (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id        BIGINT       COMMENT '关联批次',
    complainant     VARCHAR(50)  COMMENT '投诉人',
    phone           VARCHAR(20)  COMMENT '联系电话',
    type            VARCHAR(50)  COMMENT '投诉类型',
    content         TEXT         COMMENT '投诉内容',
    images          VARCHAR(500) COMMENT '凭证图片（逗号分隔）',
    status          VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING待处理/PROCESSING处理中/CLOSED已关闭',
    reply           TEXT         COMMENT '处理回复',
    consumer_reply  TEXT         COMMENT '消费者确认回复',
    consumer_reply_time DATETIME COMMENT '消费者确认时间',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    reply_time      DATETIME,
    INDEX idx_complaint_batch (batch_id),
    INDEX idx_complaint_status (status),
    CONSTRAINT fk_complaint_batch FOREIGN KEY (batch_id) REFERENCES crop_batch(id)
) COMMENT '投诉表';

-- ----------------------------
-- 7. 扫码记录表（消费者每次扫码溯源都记一条）
-- ----------------------------
CREATE TABLE scan_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id    BIGINT       COMMENT '关联批次ID',
    trace_code  VARCHAR(100) COMMENT '溯源码',
    ip          VARCHAR(50)  COMMENT '扫码IP',
    user_agent  VARCHAR(500) COMMENT '设备/浏览器信息',
    scan_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '扫码时间',
    INDEX idx_scan_log_batch (batch_id),
    INDEX idx_scan_log_time (scan_time),
    CONSTRAINT fk_scan_log_batch FOREIGN KEY (batch_id) REFERENCES crop_batch(id)
) COMMENT '扫码记录表';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 测试数据
-- =============================================

-- 用户：admin/123456, farmer01/123456, dealer01/123456
-- 密码都是 123456 的MD5：e10adc3949ba59abbe56e057f20f883e
-- 应用启动时 PasswordMigrationRunner 会自动迁移为 BCrypt
INSERT INTO sys_user (username, password, real_name, role, phone, address) VALUES
('admin',    'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 'admin',   '13800000000', '平台后台'),
('farmer01', 'e10adc3949ba59abbe56e057f20f883e', '张建国',     'farmer',  '13900000001', '山东省寿光市洛城街道'),
('farmer02', 'e10adc3949ba59abbe56e057f20f883e', '李秀兰',     'farmer',  '13900000002', '山东省寿光市孙家集街道'),
('dealer01', 'e10adc3949ba59abbe56e057f20f883e', '绿丰果蔬批发', 'dealer',  '13700000001', '寿光农产品物流园A区12号'),
('dealer02', 'e10adc3949ba59abbe56e057f20f883e', '鲜达商超采购', 'dealer',  '13700000002', '哈尔滨市哈达果蔬批发市场');

-- 种植批次
INSERT INTO crop_batch (batch_no, farmer_id, crop_name, variety, land_location, plant_date, expect_harvest_date, actual_harvest_date, harvest_weight, status, trace_code) VALUES
('BATCH202603001', 2, '西红柿', '粉冠一号', '山东省寿光市洛城街道张家庄村3号大棚', '2026-03-01', '2026-06-15', '2026-06-14', 2000.00, 'HARVESTED', 'TZ202603001001'),
('BATCH202604002', 3, '黄瓜',   '津优35号', '山东省寿光市孙家集街道李家庄', '2026-04-10', '2026-07-01', NULL, NULL, 'PLANTING',  NULL);

-- 农事记录
INSERT INTO farming_log (batch_id, log_type, log_date, material, dosage, content) VALUES
(1, 'SOWING',    '2026-03-01', '粉冠一号种子', '50g',  '大棚育苗播种，棚内温度25度'),
(1, 'FERTILIZE', '2026-03-20', '腐熟有机肥',   '500kg/亩', '定植前施足底肥'),
(1, 'PESTICIDE', '2026-04-15', '吡虫啉',       '20g/亩', '防治蚜虫，安全间隔期7天'),
(1, 'IRRIGATE',  '2026-05-01', NULL,           NULL,     '滴灌浇水2小时'),
(1, 'FERTILIZE', '2026-05-20', '复合肥N-P-K=15-15-15', '30kg/亩', '结果期追肥'),
(2, 'SOWING',    '2026-04-10', '津优35号黄瓜种子', '30g', '直播播种');

-- 流转记录（用户ID：admin=1 farmer01=2 farmer02=3 dealer01=4 dealer02=5）
INSERT INTO trace_record (batch_id, from_user, to_user, type, quantity, location, remark) VALUES
(1, 2, NULL, 'HARVEST',  2000, '寿光市洛城街道张家庄村3号大棚', '采收西红柿2000斤'),
(1, 2, 4,    'INBOUND',  2000, '寿光农产品物流园A区12号',       '张建国卖给绿丰果蔬批发'),
(1, 4, 5,    'OUTBOUND', 1500, '寿光物流园→哈尔滨哈达市场',     '绿丰批发发往哈尔滨鲜达商超');

-- 检测报告
INSERT INTO detection_report (batch_id, report_no, institution, report_date, result) VALUES
(1, 'TEST202606001', '寿光市农产品质量检测中心', '2026-06-10', '合格');

-- 投诉示例
INSERT INTO complaint (batch_id, complainant, phone, type, content, status) VALUES
(1, '王女士', '13600000001', '品质问题', '买的西红柿有烂果，希望核实', 'PENDING');

-- =============================================
-- 已有数据库升级（新增消费者确认字段，已有数据库需手动执行一次）
-- =============================================
ALTER TABLE complaint ADD COLUMN consumer_reply TEXT COMMENT '消费者确认回复';
ALTER TABLE complaint ADD COLUMN consumer_reply_time DATETIME COMMENT '消费者确认时间';
