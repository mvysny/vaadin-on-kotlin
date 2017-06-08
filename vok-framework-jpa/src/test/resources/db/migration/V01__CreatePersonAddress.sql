create table TestPerson(
  id BIGINT auto_increment PRIMARY KEY,
  name VARCHAR(100) not NULL,
  age INTEGER not null
);

create table TestHobby(
  id BIGINT auto_increment PRIMARY KEY,
  person_id BIGINT not null REFERENCES TestPerson(id),
  text varchar(200) not NULL
)
