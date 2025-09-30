package com.cchat.cclient;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Slf4j
@Component
public class CleanAspect {
    @Pointcut("@annotation(com.cchat.cclient.model.Clean)")
    public void cleanableMethod() {}
    
    @Before("cleanableMethod()")
    public void clean() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        log.info("Cleaned");
    }
}
