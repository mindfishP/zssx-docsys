package com.wechatapply.associationwechatapply;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.wechatapply.associationwechatapply.mapper")
public class AssociationWechatApplyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssociationWechatApplyApplication.class, args);
    }


}
