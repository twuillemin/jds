CREATE TABLE jds_group(
    id INT GENERATED ALWAYS AS IDENTITY,
    name VARCHAR NOT NULL
);

CREATE TABLE jds_user(
    id INT GENERATED ALWAYS AS IDENTITY,
    name VARCHAR NOT NULL,
    password VARCHAR NOT NULL,
    first_name VARCHAR NOT NULL,
    last_name VARCHAR NOT NULL,
    enabled BOOLEAN NOT NULL,
    profile VARCHAR NOT NULL
);

CREATE TABLE jds_group_user(
    group_id INT,
    user_id INT,
    is_admin BOOLEAN
);
