# innodb下RR隔离级别事务中存在的写写并发中写覆盖问题的解决

### 介绍
```
1。mysql数据库innodb引擎下，开启RR隔离级别的事务下，开启后写写并发中存在写覆盖问题，
2。介绍几种解决方案和特点
```
### 1.0 预先准备sql
```
create table test(
    id int auto_increment primary key,
    money bigint default 0 not null
);
insert into test (id,money) values (1,0);
```
### 2.0 在yaml中配置数据库地址和访问密码
### 3.0 运行在test包中的DemoApplicationTests
```
打印说明：
init:Account(id=1, money=0)     修改对象的初始状态
花费时间：PT0.315S                所有线程执行完毕花费的时间
final:Account(id=1, money=100)  修改对象的最终状态
执行成功率: 1.0                   所有线程执行成功率（（执行次数 - 回滚次数）/ 执行次数）
通过测试，不存在写覆盖问题           是否能有效防止写覆盖问题
发生回滚时的异常为：xxx             发生回滚时的异常
```
### 4.0 修改DemoApplicationTests中的参数，观察不同参数下各个方案的执行效果
```
可修改参数：（供参考）
1。DemoApplicationTests.times （并发修改的线程数量）
2。yaml中数据库连接池的参数
```
    

