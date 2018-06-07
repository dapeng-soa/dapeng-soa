
drop database if EXISTS demo_orderdb;
create database demo_orderdb;

drop database if EXISTS crm;
create database crm;

use demo_orderdb;

create table orders(
id INT(11) PRIMARY KEY AUTO_INCREMENT,
  order_no varchar(11) not null UNIQUE,
  status TINYINT(1) DEFAULT 0 not null,
  amount DECIMAL DEFAULT 0.0
);


