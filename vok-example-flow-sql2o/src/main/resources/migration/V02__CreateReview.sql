create TABLE Review(
  id bigint auto_increment PRIMARY KEY,
  score TINYINT NOT NULL,
  date DATE not NULL,
  category BIGINT,
  count TINYINT not null
);
alter table Review add CONSTRAINT r_score_range CHECK (score >= 1 and score <= 5);
alter table Review add CONSTRAINT r_fk_category REFERENCES Category (ID);
alter table Review add CONSTRAINT r_count_range CHECK (count >= 1 and count <= 99);
