# innodb下RR隔离级别事务中存在的写写并发中写覆盖问题的解决(在springboot中测试)

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

# mysql数据库innodb存储引擎下事务并发问题的介绍
### 如果对事务acid和事务隔离级别不太了解的同学，推荐一下这篇文章https://blog.csdn.net/weixin_44015043/article/details/105217603
## 1。简单介绍事务的隔离级别作用
    在mysql，innodb引擎下，事务支持:
    1.RU（读未提交，存在脏读）
    2.RC（读已提交）
        解决脏读，存在不可重复读问题，前后两次读到的数据不一致，即本事务的逻辑会被其他事务影响
    3.RR（可重复读）
        解决不可重复读，存在业务逻辑并发问题，
        例子：一个扣费和转账业务同时进行，最终结果为其中某个业务执行成功的结果，先完成的提交被后完成的提交覆盖
    4.SERIALIZABLE（串行化）
        满足事务ACID的完整定义，类似于synchronized,读即加表锁，阻塞其他事务对这个表的读操作
## 2。简单介绍我们要测试的问题
    业务需求：并发情况下自增一条数据的一个字段
## 3。解决方案：
	0。错误示范，有并发问题
        开启隔离级别为可重复读（mysql默认事务隔离级别）的事务，
        读取原数据，将读取的数据自增后更新覆盖原数据
        特点：并发下，不同线程中，先开始的更新操作可能会覆盖后开始的更新操作，发生写覆盖问题
    1。开启隔离级别为串行化的事务，
        读取原数据，将读取的数据自增后更新覆盖原数据
        优点：1。保证事务一个接一个运行，保证数据一致性
        缺点  1。锁全表，影响其他线程读操作
             2。多线程环境下尝试获取锁时会发现死锁；事务执行成功率低，执行效率也低
             3。一般不建议使用
         
    2。开启隔离级别为可重复读的事务，
        执行sql：update account set money = money + #{money} where id = #{id}
        优点：1。对于简单逻辑无需事务
            2。执行效率高
        缺点：1。只适用于简单逻辑，如无法做是否超卖之类的判断
       
    3。不开启事务，
       使用cas加循环的方式，不断循环尝试更新
       优点：1。一定程度保证数据的一致性
            2。低并发量下执行效率高
       缺点：1。需要手动写重试逻辑
            2。高并发下难以命中正确的修改，执行效率低下，且随着并发量的增加而大量下降，低命中率的重试导致数据库资源被浪费
            3。复杂业务逻辑下需要数据库增加version字段，
            4。因复杂的逻辑中有回滚需求加入事务时，只能使用读未提交才能使用cas
           
    4。开启隔离级别为可重复读的事务，
        读取原数据手动加上互斥锁，将读取的数据自增后更新覆盖原数据
        优点：1。通过给读取的数据加上排他锁，保证数据一致性
           2。锁家在单条数据上，对其他不设计此条信息的读操作不造成影响
           3。效率较高，在各个并发量下有稳定的性能
        缺点：1。必须手动在读操作时加锁，需要枷锁的内容由程序原决定，在复杂业务中需要人为确认哪些读操作要加锁
    

