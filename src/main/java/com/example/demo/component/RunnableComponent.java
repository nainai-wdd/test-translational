package com.example.demo.component;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.mapper.Account;
import com.example.demo.mapper.AccountMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author nainai
 */
@Component
public class RunnableComponent{

    /**
     * 同步辅助工具，所有调用过该对象await方法的线程会暂停，
     * 在countDownLatch对象被调用countDown方法1次后所有被该对象await方法暂停的线程同步开始
     */
    private CountDownLatch countDownLatch;

    private CountDownLatch countDownLatchEnd;

    @Resource
    private AccountMapper accountMapper;

    /**
     * 数据的id
     */
    private final Long id = 1L;

    /**
     * 每次增加的金额
     */
    private final Long addMoney = 1L;

    /**
     * 回滚时发生的异常
     */
    private Exception rollException;

    /**
     * 同时开启的线程数量
     */
    private int times;

    /**
     * 执行方法成功的次数
     */
    private volatile AtomicInteger execute = new AtomicInteger(0);

    public void init(Integer times) {
        this.rollException = null;
        this.times = times;
        this.countDownLatch = new CountDownLatch(1);
        this.countDownLatchEnd = new CountDownLatch(times);
        this.execute.set(0);
    }


    public void start() {
        countDownLatch.countDown();
    }

    public void startLogTime() {
        try {
            countDownLatchEnd.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    public Runnable run(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
                execute.incrementAndGet();
            } catch (Exception e) {
                if (rollException == null) {
                    this.rollException = e;
                }
            }

        };
    }


    /**
     * 错误示范，有并发问题
     * 开启隔离级别为可重复读（mysql默认事务隔离级别）的事务，
     * 读取原数据，将读取的数据自增后更新覆盖原数据
     * 特点：并发下，不同线程中，先开始的更新操作可能会覆盖后开始的更新操作，发生写覆盖问题
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public void run5Exception() {
        // 准备
        pre();
        try {
            // 核心方法
            Account account = accountMapper.selectById(id);
            account.setMoney(account.getMoney() + addMoney);
            accountMapper.updateById(account);
            // 提示执行完毕，为了记录时间
        } finally {
            countDownLatchEnd.countDown();
        }
    }

    /**
     * 开启隔离级别为串行化的事务，
     * 读取原数据，将读取的数据自增后更新覆盖原数据
     * 优点：1。保证事务一个接一个运行，保证数据一致性
     * 缺点  1。锁全表，影响其他线程读操作
     *      2。多线程环境下尝试获取锁时会发现死锁；事务执行成功率低，执行效率也低
     *      3。一般不建议使用
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void runSerializable(){
        // 准备
        pre();
        try {
            // 核心方法
            Account account = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                    .eq(Account::getId, id));
            account.setMoney(account.getMoney() + addMoney);
            accountMapper.updateById(account);
        } finally {
            // 记录时间
            countDownLatchEnd.countDown();
        }
    }

    /**
     * 开启隔离级别为可重复读的事务，
     *  执行sql：update account set money = money + #{money} where id = #{id}
     *  优点：1。对于简单逻辑无需事务
     *       2。执行效率高
     *  缺点：1。只适用于简单逻辑，如无法做是否超卖之类的判断
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public void runSQL(){
        // 准备
        pre();
        try {
            // 核心方法
            accountMapper.addMoney(addMoney, id);
        } finally {
            //
            countDownLatchEnd.countDown();
        }
    }

    /**
     * 不开启事务，
     * 使用cas加循环的方式，不断循环尝试更新
     * 优点：1。一定程度保证数据的一致性
     *      2。低并发量下执行效率高
     * 缺点：1。需要手动写重试逻辑
     *      2。高并发下难以命中正确的修改，执行效率低下，且随着并发量的增加而大量下降，低命中率的重试导致数据库资源被浪费
     *      3。复杂业务逻辑下需要数据库增加version字段，
     *      4。因复杂的逻辑中有回滚需求加入事务时，只能使用读已提交才能使用cas
     */
    public void runCAS(){
        // 准备
        pre();
        try {
            // 核心方法
            int update = 0;
            while (update == 0) {
                Account account = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                        .eq(Account::getId, id));
                update = accountMapper.update(null, Wrappers.<Account>lambdaUpdate()
                        .set(Account::getMoney, account.getMoney() + addMoney)
                        .eq(Account::getMoney, account.getMoney())
                        .eq(Account::getId, account.getId()));
            }
        } finally {
            // 提示执行完毕，为了记录时间
            countDownLatchEnd.countDown();
        }
    }

    /**
     * 开启隔离级别为可重复读的事务，
     * 读取原数据手动加上互斥锁，将读取的数据自增后更新覆盖原数据
     * 优点：1。通过给读取的数据加上排他锁，保证数据一致性
     *      2。锁家在单条数据上，对其他不设计此条信息的读操作不造成影响
     *      3。效率较高，在各个并发量下有稳定的性能
     * 缺点：1。必须手动在读操作时加锁，需要枷锁的内容由程序原决定，在复杂业务中需要人为确认哪些读操作要加锁
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public void runForUpdate(){
        // 准备
        pre();
        try {
            // 核心方法
            Account account = accountMapper.selectOne(Wrappers.<Account>lambdaQuery()
                    .eq(Account::getId, id)
                    .last(" for update"));
            account.setMoney(account.getMoney() + addMoney);
            accountMapper.updateById(account);
        } finally {
            //记录时间用
            countDownLatchEnd.countDown();
        }
    }




    private void pre() {
        //统一各个线程的开始时间
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public Long getId() {
        return id;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public boolean result() {
        Account account = accountMapper.selectById(id);
        System.out.println("final:" + account);
        System.out.println("执行成功率: " + (execute.doubleValue() / times) + ", 期待money值：" + execute.get() * addMoney);
        boolean result = account.getMoney().equals(execute.get() * addMoney);
        if (result) {
            System.out.println("通过测试，不存在写覆盖问题");
        } else {
            System.out.println("未通过测试，存在并发问题");
        }
        if (rollException != null) {
            System.out.println("发生回滚时的异常为：" + rollException.getMessage());
        }
        return result;
    }


}
