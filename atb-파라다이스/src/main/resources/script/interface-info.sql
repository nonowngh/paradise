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
WHERE info.interface_id = 'IF_001';
