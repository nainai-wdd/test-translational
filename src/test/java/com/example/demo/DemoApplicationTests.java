package com.example.demo;

import com.example.demo.component.RunnableComponent;
import com.example.demo.mapper.Account;
import com.example.demo.mapper.AccountMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class DemoApplicationTests {

    @Resource
    private RunnableComponent runnableComponent;

    @Resource
    private AccountMapper accountMapper;

    /**
     * 手动设置线程数
     */
    private final int times = 100;


    @Test
    void testTransnationals(){
        accountMapper.selectById(1);
        System.out.println("============================");
        System.out.println("测试写并发中写覆盖问题，总线程数：" + times);

        System.out.println("============================");
        System.out.println("开始测试ForUpdate模式：");
        testTransnational(runnableComponent::runForUpdate);

        System.out.println("============================");
        System.out.println("开始测试SQL模式：");
        testTransnational(runnableComponent::runSQL);

        System.out.println("============================");
        System.out.println("开始测试CAS模式：");
        testTransnational(runnableComponent::runCAS);

        System.out.println("============================");
        System.out.println("开始测试Serializable模式：");
        testTransnational(runnableComponent::runSerializable);

        System.out.println("============================");
        System.out.println("开始测试错误示范模式：");
        testTransnational(runnableComponent::run5Exception);
    }


    private void testTransnational(Runnable runnable) {
        // 初始化状态，（清零）
        initDate();
        // 设置批量任务
        ExecutorService executorService = Executors.newFixedThreadPool(times);
        for (int i = 0; i < times; i++) {
            executorService.execute(runnableComponent.run(runnable));
        }
        // 开启任务，记录花费时间
        logSpendTime();
        finalAssert();
    }

    private void initDate() {
        // 初始化 runnableComponent
        runnableComponent.init(times);
        //手动初始化状态
        Account account1 = new Account();
        account1.setId(runnableComponent.getId());
        account1.setMoney(0L);
        accountMapper.updateById(account1);
        //断言初始状态
        Account old = accountMapper.selectById(runnableComponent.getId());
        System.out.println("init:" + old);
        assert old.getMoney().equals(0L);
    }

    private void logSpendTime(){
        runnableComponent.start();
        LocalDateTime start = LocalDateTime.now();
        runnableComponent.startLogTime();
        Duration between = Duration.between(start, LocalDateTime.now());
        System.out.println("花费时间：" + between);
    }

    private void finalAssert() {
        //断言最终状态
        assert runnableComponent.result();
        //清理状态
//        initDate();
    }
}
