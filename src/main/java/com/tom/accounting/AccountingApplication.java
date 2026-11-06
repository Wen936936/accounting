package com.tom.accounting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 应用启动类，@SpringBootApplication 开启自动配置和组件扫描
@SpringBootApplication
public class AccountingApplication {
    // 程序入口
    public static void main(String[] args) {
        SpringApplication.run(AccountingApplication.class, args);
    }
}
