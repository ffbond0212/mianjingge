package com.mianjingge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mianjingge.mapper")
public class MianjinggeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MianjinggeApplication.class, args);
    }
}
