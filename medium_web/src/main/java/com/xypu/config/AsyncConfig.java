package com.xypu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 开启异步任务与定时任务支持 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
