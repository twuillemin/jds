CREATE TABLE IF NOT EXISTS jds_server
(
    id                    SERIAL PRIMARY KEY,
    type                  VARCHAR NOT NULL,
    name                  VARCHAR NOT NULL,
    group_id              BIGINT  NOT NULL,
    customer_defined      BOOLEAN NOT NULL,
    sql_jdbc_url          VARCHAR DEFAULT NULL,
    sql_user_name         VARCHAR DEFAULT NULL,
    sql_password          VARCHAR DEFAULT NULL,
    sql_driver_class_name VARCHAR DEFAULT NULL,
    gsheet_workbook_url   VARCHAR DEFAULT NULL,
    gsheet_user_name      VARCHAR DEFAULT NULL,
    gsheet_password       VARCHAR DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS jds_schema
(
    id            SERIAL PRIMARY KEY,
    type          VARCHAR NOT NULL,
    name          VARCHAR NOT NULL,
    server_id     BIGINT  NOT NULL REFERENCES jds_server (id),
    sql_role_name VARCHAR DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS jds_dataprovider
(
    id                SERIAL PRIMARY KEY,
    type              VARCHAR NOT NULL,
    name              VARCHAR NOT NULL,
    schema_id         BIGINT  NOT NULL REFERENCES jds_schema (id),
    editable          BOOLEAN NOT NULL,
    sql_query         VARCHAR DEFAULT NULL,
    gsheet_sheet_name VARCHAR DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS jds_dataprovider_column
(
    data_provider_id        BIGINT  NOT NULL REFERENCES jds_dataprovider (id),
    column_index            INT     NOT NULL,
    type                    VARCHAR NOT NULL,
    name                    VARCHAR NOT NULL,
    data_type               VARCHAR NOT NULL,
    size                    INT     NOT NULL,
    lookup_maximum_number   INT              DEFAULT NULL,
    lookup_data_source_id   BIGINT           DEFAULT NULL,
    lookup_key_column       VARCHAR          DEFAULT NULL,
    lookup_value_column     VARCHAR          DEFAULT NULL,
    storage_type            VARCHAR NOT NULL,
    storage_nullable        BOOLEAN NOT NULL DEFAULT FALSE,
    storage_primary_key     BOOLEAN NOT NULL DEFAULT FALSE,
    storage_auto_increment  BOOLEAN NOT NULL DEFAULT FALSE,
    storage_read_attr_name  VARCHAR NOT NULL,
    storage_write_attr_name VARCHAR          DEFAULT NULL,
    storage_container_name  VARCHAR          DEFAULT NULL,
    PRIMARY KEY (data_provider_id, column_index)
);

CREATE TABLE IF NOT EXISTS jds_datasource
(
    id               SERIAL PRIMARY KEY,
    name             VARCHAR NOT NULL,
    data_provider_id BIGINT  NOT NULL REFERENCES jds_dataprovider (id)
);

CREATE TABLE IF NOT EXISTS jds_datasource_user
(
    data_source_id BIGINT  NOT NULL REFERENCES jds_datasource (id),
    user_id        BIGINT  NOT NULL,
    permission     VARCHAR NOT NULL,
    PRIMARY KEY (data_source_id, user_id)
);