package com.example.demo.component;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.demo.mapper.Test;
import com.example.demo.mapper.TestMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * @author nainai
 */
@Component
public class RunnableComponent{

    /**
     * 同步辅助工具，所有调用过该对象await方法的线程会暂停，
     * 在countDownLatch对象被调用countDown方法1次后所有被该对象await方法暂停的线程同步开始
     */
    private static final CountDownLatch countDownLatch = new CountDownLatch(1);

    public static void start() {
        countDownLatch.countDown();
    }

    @Resource
    private TestMapper testMapper;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public void run1() throws InterruptedException {
        countDownLatch.await();
        Test test = testMapper.selectOne(Wrappers.<Test>lambdaQuery()
                .eq(Test::getId, 1)
                .last(" for update"));
        test.setMoney(test.getMoney() + 1);
        testMapper.updateById(test);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public void run2() throws InterruptedException {
        countDownLatch.await();
        Test test = testMapper.selectOne(Wrappers.<Test>lambdaQuery()
                .eq(Test::getId, 1));
        test.setMoney(test.getMoney() + 1);
        testMapper.updateById(test);
    }

    public void run3() throws InterruptedException {
        countDownLatch.await();
        int update = 0;
        while (update == 0) {
            Test test = testMapper.selectOne(Wrappers.<Test>lambdaQuery()
                    .eq(Test::getId, 1));
            update = testMapper.update(null, Wrappers.<Test>lambdaUpdate()
                    .set(Test::getMoney, test.getMoney() + 1)
                    .eq(Test::getMoney, test.getMoney())
                    .eq(Test::getId, test.getId()));
        }
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public void run4() throws InterruptedException {
        countDownLatch.await();
        testMapper.addMoney(10, 1);
        System.out.println(testMapper.selectById(1));
    }

    /**
     * 错误示范，有并发问题
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public void run5Exception() throws InterruptedException {
        countDownLatch.await();
        Test test = testMapper.selectById(1);
        test.setMoney(test.getMoney() + 10);
        testMapper.updateById(test);
    }



}
