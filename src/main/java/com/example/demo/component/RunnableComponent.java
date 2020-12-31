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
     * 读取原数据手动加上互斥锁，将读取的数据自增后更新覆盖原数据
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


    /**
     * 不开启事务，
     * 使用cas加循环的方式，不断循环尝试更新
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
     *  执行sql：update account set money = money + #{money} where id = #{id}
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
        System.out.println("执行成功率: " + (execute.doubleValue() / times));
        if (rollException != null) {
            System.out.println("发生回滚时的异常为：" + rollException.getMessage());
        }
        boolean result = account.getMoney().equals(execute.get() * addMoney);
        if (result) {
            System.out.println("通过测试，不存在写覆盖问题");
        } else {
            System.out.println("未通过测试，存在并发问题");
        }
        return result;
    }


}
