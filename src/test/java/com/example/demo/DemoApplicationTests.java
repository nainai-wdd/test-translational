package com.example.demo;

import com.example.demo.component.RunnableComponent;
import com.example.demo.mapper.TestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class DemoApplicationTests {

    @Resource
    private RunnableComponent runnableComponent;

    private final int times = 400;

    @Test
    void contextLoads() throws InterruptedException {
        Runnable runnable = () ->{
            try {
                runnableComponent.run4();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("执行成功");
        };
        ExecutorService executorService = Executors.newFixedThreadPool(times);
        for (int i = 0; i < times; i++) {
            executorService.execute(runnable);
        }
        RunnableComponent.start();
        Thread.sleep(100000);
    }

}
