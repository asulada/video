package com.asuala.mock;

import cn.hutool.http.HttpGlobalConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.io.IOException;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.asuala.mock.mapper") //扫描mapper的包，或者读者可以在对应的mapper上加上@Mapper的注解
public class MockApplication {


    public static void main(String[] args) throws IOException {
        HttpGlobalConfig.setTimeout(10000);
        SpringApplication.run(MockApplication.class, args);
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.initialize();
        return taskScheduler;
    }
}
