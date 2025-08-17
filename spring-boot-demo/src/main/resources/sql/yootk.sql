drop database if exists yootk;
create database yootk character set utf8;
use yootk;
create table course (
  cid bigint auto_increment comment '课程id',
  cname VARCHAR(50) comment '课程名称',
  start date comment '课程开始日期',
  end date comment '课程结束日期',
  credit int comment '课程学分',
  num int comment '课程人数',
  constraint pk_cid primary key(cid)
)engine=innodb;