create TABLE CATEGORY (
  id bigint auto_increment PRIMARY KEY,
  name varchar(200) NOT NULL,
);
create UNIQUE INDEX idx_category_name ON CATEGORY(name);
