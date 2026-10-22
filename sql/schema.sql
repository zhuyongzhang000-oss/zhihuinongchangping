-- =============================================================
-- 智慧农业农产品溯源管理系统 · 建表脚本
-- MySQL 8.0+（字符集 utf8mb4）
-- 用法：mysql -uroot -p < sql/schema.sql   然后再执行 sql/data.sql
-- =============================================================
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS agri_trace DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE agri_trace;

SET FOREIGN_KEY_CHECKS = 0;

-- 用户表：ADMIN 管理员 / FARMER 生产者 / INSPECTOR 质检员 / LOGISTICS 物流员
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL COMMENT '登录名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
    real_name   VARCHAR(50)  NOT NULL COMMENT '姓名',
    phone       VARCHAR(20)  DEFAULT NULL,
    role        VARCHAR(20)  NOT NULL COMMENT 'ADMIN/FARMER/INSPECTOR/LOGISTICS',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT = '系统用户';

-- 种植基地
DROP TABLE IF EXISTS farm_base;
CREATE TABLE farm_base (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    code        VARCHAR(30)   NOT NULL COMMENT '基地编号',
    name        VARCHAR(100)  NOT NULL COMMENT '基地名称',
    address     VARCHAR(200)  DEFAULT NULL COMMENT '地址',
    area        DECIMAL(10,2) DEFAULT NULL COMMENT '面积（亩）',
    manager     VARCHAR(50)   DEFAULT NULL COMMENT '负责人',
    phone       VARCHAR(20)   DEFAULT NULL,
    environment VARCHAR(200)  DEFAULT NULL COMMENT '环境说明（土壤/水源/气候）',
    description VARCHAR(500)  DEFAULT NULL,
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '1 使用中 0 停用',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB COMMENT = '种植基地';

-- 农产品（品类档案）
DROP TABLE IF EXISTS product;
CREATE TABLE product (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL COMMENT '产品名称',
    category        VARCHAR(30)  NOT NULL COMMENT '类别：蔬菜/水果/粮油/茶叶/调味品/其他',
    variety         VARCHAR(100) DEFAULT NULL COMMENT '品种',
    unit            VARCHAR(10)  NOT NULL DEFAULT 'kg' COMMENT '计量单位',
    shelf_life_days INT          DEFAULT NULL COMMENT '保质期（天）',
    storage         VARCHAR(200) DEFAULT NULL COMMENT '储存方式',
    description     VARCHAR(500) DEFAULT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '农产品';

-- 生产批次：溯源的核心单位
-- 状态流转：GROWING 种植中 → HARVESTED 已采收 → INSPECTED 质检合格 / REJECTED 质检不合格 → SHIPPING 运输中 → ON_SALE 已上市
DROP TABLE IF EXISTS batch;
CREATE TABLE batch (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    batch_no     VARCHAR(40)   NOT NULL COMMENT '批次号',
    product_id   BIGINT        NOT NULL,
    base_id      BIGINT        NOT NULL,
    plant_date   DATE          DEFAULT NULL COMMENT '种植日期',
    harvest_date DATE          DEFAULT NULL COMMENT '采收日期',
    quantity     DECIMAL(12,2) DEFAULT NULL COMMENT '产量',
    status       VARCHAR(20)   NOT NULL DEFAULT 'GROWING',
    remark       VARCHAR(500)  DEFAULT NULL,
    created_by   BIGINT        DEFAULT NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batch_no),
    KEY idx_product (product_id),
    KEY idx_base (base_id)
) ENGINE = InnoDB COMMENT = '生产批次';

-- 农事记录（种植过程）
DROP TABLE IF EXISTS farming_record;
CREATE TABLE farming_record (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    batch_id    BIGINT       NOT NULL,
    type        VARCHAR(20)  NOT NULL COMMENT 'SOW 播种/定植 FERTILIZE 施肥 IRRIGATE 灌溉 PEST 病虫害防治 WEED 除草 HARVEST 采收 OTHER 其他',
    op_date     DATE         NOT NULL COMMENT '作业日期',
    operator    VARCHAR(50)  DEFAULT NULL COMMENT '作业人',
    material    VARCHAR(100) DEFAULT NULL COMMENT '投入品（种子/肥料/农药名称）',
    dosage      VARCHAR(50)  DEFAULT NULL COMMENT '用量',
    description VARCHAR(500) DEFAULT NULL,
    created_by  BIGINT       DEFAULT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_batch (batch_id)
) ENGINE = InnoDB COMMENT = '农事记录';

-- 质检记录
DROP TABLE IF EXISTS inspection;
CREATE TABLE inspection (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    batch_id          BIGINT       NOT NULL,
    report_no         VARCHAR(40)  NOT NULL COMMENT '检测报告编号',
    inspect_date      DATE         NOT NULL,
    agency            VARCHAR(100) DEFAULT NULL COMMENT '检测机构',
    inspector         VARCHAR(50)  DEFAULT NULL COMMENT '检测人',
    pesticide_residue VARCHAR(50)  DEFAULT NULL COMMENT '农药残留',
    heavy_metal       VARCHAR(50)  DEFAULT NULL COMMENT '重金属',
    other_items       VARCHAR(300) DEFAULT NULL COMMENT '其他检测项',
    result            VARCHAR(10)  NOT NULL COMMENT 'PASS 合格 / FAIL 不合格',
    conclusion        VARCHAR(500) DEFAULT NULL,
    created_by        BIGINT       DEFAULT NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_no (report_no),
    KEY idx_batch (batch_id)
) ENGINE = InnoDB COMMENT = '质检记录';

-- 物流记录
DROP TABLE IF EXISTS logistics;
CREATE TABLE logistics (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    batch_id     BIGINT       NOT NULL,
    waybill_no   VARCHAR(40)  NOT NULL COMMENT '运单号',
    carrier      VARCHAR(100) DEFAULT NULL COMMENT '承运单位',
    vehicle_no   VARCHAR(20)  DEFAULT NULL COMMENT '车牌号',
    driver       VARCHAR(50)  DEFAULT NULL,
    driver_phone VARCHAR(20)  DEFAULT NULL,
    from_addr    VARCHAR(200) NOT NULL COMMENT '起运地',
    to_addr      VARCHAR(200) NOT NULL COMMENT '目的地',
    depart_time  DATETIME     NOT NULL COMMENT '发车时间',
    arrive_time  DATETIME     DEFAULT NULL COMMENT '到达时间',
    temperature  VARCHAR(30)  DEFAULT NULL COMMENT '运输温度',
    status       VARCHAR(20)  NOT NULL DEFAULT 'IN_TRANSIT' COMMENT 'IN_TRANSIT 运输中 / ARRIVED 已到达',
    remark       VARCHAR(500) DEFAULT NULL,
    created_by   BIGINT       DEFAULT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_waybill_no (waybill_no),
    KEY idx_batch (batch_id)
) ENGINE = InnoDB COMMENT = '物流记录';

-- 溯源码：一个批次一个码，消费者扫码查看整条链
DROP TABLE IF EXISTS trace_code;
CREATE TABLE trace_code (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    code         VARCHAR(32) NOT NULL COMMENT '溯源码',
    batch_id     BIGINT      NOT NULL,
    scan_count   INT         NOT NULL DEFAULT 0,
    last_scan_at DATETIME    DEFAULT NULL,
    status       TINYINT     NOT NULL DEFAULT 1 COMMENT '1 有效 0 作废',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    UNIQUE KEY uk_batch (batch_id)
) ENGINE = InnoDB COMMENT = '溯源码';

-- 扫码日志：用于统计扫码趋势
DROP TABLE IF EXISTS trace_scan_log;
CREATE TABLE trace_scan_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(32)  NOT NULL,
    batch_id   BIGINT       NOT NULL,
    ip         VARCHAR(64)  DEFAULT NULL,
    user_agent VARCHAR(300) DEFAULT NULL,
    scanned_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_scanned_at (scanned_at),
    KEY idx_code (code)
) ENGINE = InnoDB COMMENT = '扫码日志';

SET FOREIGN_KEY_CHECKS = 1;
