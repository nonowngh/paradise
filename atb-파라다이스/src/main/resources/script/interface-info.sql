TB_INTERFACE_INFO
(INTERFACE_ID,
CRON_EXPRESSION,
PATTERN_TYPE,
SEND_SYSTEM_CODE,
RECV_SYSTEM_CODE,
MAPPING_YN)

TB_INTERFACE_DETAIL
(INTERFACE_ID,
PROPERTY_NAME,
PROPERTY_VALUE)

TB_INTERFACE_QUERY
(INTERFACE_ID,
SQL_ID,
QUERY)


CREATE TABLE TB_INTERFACE_INFO (
    INTERFACE_ID       VARCHAR(30)  PRIMARY KEY,
    CRON_EXPRESSION    VARCHAR(30) NOT NULL,
    PATTERN_TYPE       VARCHAR(10)  NOT NULL,
    SEND_SYSTEM_CODE   CHAR(3)  NOT NULL,
    RECV_SYSTEM_CODE   CHAR(3)  NOT NULL,
    MAPPING_YN         CHAR(1)      DEFAULT 'N' NOT NULL CHECK (MAPPING_YN IN ('Y', 'N')),
    USE_YN             CHAR(1)      DEFAULT 'Y' NOT NULL CHECK (USE_YN IN ('Y', 'N')),
--    STATUS             VARCHAR(20)  DEFAULT 'READY',  -- 예: READY, RUNNING, ERROR 등
--    DESCRIPTION        VARCHAR(500),
    CREATED_AT         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY         VARCHAR(50),
    UPDATED_BY         VARCHAR(50)
);

CREATE TABLE TB_INTERFACE_DETAIL (
    INTERFACE_ID     VARCHAR(30)  NOT NULL,
    PROPERTY_NAME    VARCHAR(100) NOT NULL,
    PROPERTY_VALUE   VARCHAR(1000),
    CREATED_AT       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY       VARCHAR(50),
    UPDATED_BY       VARCHAR(50),
    PRIMARY KEY (INTERFACE_ID, PROPERTY_NAME),
    FOREIGN KEY (INTERFACE_ID) REFERENCES TB_INTERFACE_INFO(INTERFACE_ID)
        ON DELETE CASCADE
);

CREATE TABLE TB_INTERFACE_QUERY (
    INTERFACE_ID   VARCHAR(30)  NOT NULL,
    SQL_ID         VARCHAR(50)  NOT NULL,
    QUERY          CLOB         NOT NULL,  -- 긴 SQL을 저장하기 위한 CLOB 타입 사용

    CREATED_AT     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY     VARCHAR(50),
    UPDATED_BY     VARCHAR(50),

    PRIMARY KEY (INTERFACE_ID, SQL_ID),
    FOREIGN KEY (INTERFACE_ID) REFERENCES TB_INTERFACE_INFO(INTERFACE_ID)
        ON DELETE CASCADE
);

INSERT INTO interface.tb_interface_info
(interface_id, cron_expression, pattern_type, send_system_code, recv_system_code, mapping_yn, use_yn, created_at, updated_at, created_by, updated_by)
VALUES('IF_UAS_CB_TRS_002', '0 8 7,17 * * ?', 'D2D', 'EAS', 'PCB', 'N', 'Y', '2025-10-01 00:15:38.889', '2025-10-01 00:15:38.889', NULL, NULL);

INSERT INTO interface.tb_interface_detail
(interface_id, property_name, property_value, created_at, updated_at, created_by, updated_by)
VALUES('IF_UAS_CB_TRS_002', 'DB_SEND_TABLE_NAMES', 'VIEW_CARD_CASINO', '2025-10-01 00:21:26.431', '2025-10-01 00:21:26.431', NULL, NULL);
INSERT INTO interface.tb_interface_detail
(interface_id, property_name, property_value, created_at, updated_at, created_by, updated_by)
VALUES('IF_UAS_CB_TRS_002', 'DB_RECV_TABLE_NAMES', 'EAIT_CARD_CASINO', '2025-10-01 00:35:14.853', '2025-10-01 00:35:14.853', NULL, NULL);
INSERT INTO interface.tb_interface_detail
(interface_id, property_name, property_value, created_at, updated_at, created_by, updated_by)
VALUES('IF_UAS_CB_TRS_002', 'DB_WORK_TYPE', 'DI', '2025-10-01 00:21:26.431', '2025-10-01 00:21:26.431', NULL, NULL);

INSERT INTO interface.tb_interface_query
(interface_id, sql_id, query, created_at, updated_at, created_by, updated_by)
VALUES('IF_UAS_CB_TRS_002', 'SELECT.VIEW_CARD_CASINO', 'SELECT * FROM VIEW_CARD_CASINO WHERE BUSINESS_AREA_CODE= ''B110'' OR BUSINESS_AREA_CODE= ''B212''', '2025-10-01 00:27:44.588', '2025-10-01 00:27:44.588', NULL, NULL);
INSERT INTO interface.tb_interface_query
(interface_id, sql_id, query, created_at, updated_at, created_by, updated_by)
VALUES('IF_UAS_CB_TRS_002', 'DELETE.EAIT_CARD_CASINO', 'DELETE EAIT_CARD_CASINO', '2025-10-01 00:27:44.588', '2025-10-01 00:27:44.588', NULL, NULL);
INSERT INTO interface.tb_interface_query
(interface_id, sql_id, query, created_at, updated_at, created_by, updated_by)
VALUES('IF_UAS_CB_TRS_002', 'INSERT.EAIT_CARD_CASINO', 'INSERT INTO EAIT_CARD_CASINO (
            CARD_NO,            
            BUSINESS_AREA_CODE,
            CARD_VENDOR_CODE,
            ORIGINAL_EMP_NO,
            EMP_NO
        )VALUES (
            #{CARD_NO},
            #{BUSINESS_AREA_CODE},
            #{CARD_VENDOR_CODE},
            #{ORIGINAL_EMP_NO},
            #{EMP_NO}
        )
', '2025-10-01 00:27:44.588', '2025-10-01 00:27:44.588', NULL, NULL);

SELECT 
    info.interface_id,
    info.cron_expression,
    info.pattern_type,
    info.send_system_code,
    info.recv_system_code,
    info.mapping_yn,
    info.use_yn,
    detail.property_name,
    detail.property_value,
    query.sql_id,
    query.query
FROM interface.tb_interface_info info
LEFT JOIN interface.tb_interface_detail detail
    ON info.interface_id = detail.interface_id
LEFT JOIN interface.tb_interface_query query
    ON info.interface_id = query.interface_id
WHERE info.interface_id = 'IF_001' and info.use_yn = 'Y';
