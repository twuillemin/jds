CREATE TABLE IF NOT EXISTS jds_group
(
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS jds_user
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    name       VARCHAR NOT NULL,
    password   VARCHAR NOT NULL,
    first_name VARCHAR NOT NULL,
    last_name  VARCHAR NOT NULL,
    enabled    BOOLEAN NOT NULL,
    profile    VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS jds_group_user
(
    group_id BIGINT  NOT NULL REFERENCES jds_group (id),
    user_id  BIGINT  NOT NULL REFERENCES jds_user (id),
    is_admin BOOLEAN NOT NULL,
    PRIMARY KEY (group_id, user_id)
);

