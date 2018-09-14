create TABLE Person(
  id bigint auto_increment PRIMARY KEY,
  PERSON_NAME varchar(200) NOT NULL,
  age INTEGER not NULL,
  dateOfBirth DATE,
  created TIMESTAMP,
  maritalStatus VARCHAR(50) NOT NULL,
  alive BOOLEAN NOT NULL
);
