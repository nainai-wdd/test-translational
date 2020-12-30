# innodb下RR隔离级别事务中存在的写写并发中写覆盖问题的解决

### 介绍
```
1。mysql数据库innodb引擎下，开启RR隔离级别的事务下，开启后写写并发中存在写覆盖问题，
2。介绍几种解决方案和特点
```
### 预先准备sql
```
create table test(
    id int auto_increment primary key,
    money bigint default 0 not null
);
insert into test (id,money) values (1,0);
```
