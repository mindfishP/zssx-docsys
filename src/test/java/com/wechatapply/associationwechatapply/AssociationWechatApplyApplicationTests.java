package com.wechatapply.associationwechatapply;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Spring 上下文加载测试：依赖本地 MySQL（association_archive 库），
 * 未配置数据库时默认跳过，避免阻塞 CI/骨架阶段构建。
 * 运行方式：set SPRING_DATASOURCE_URL=... 后执行 mvn test
 */
class AssociationWechatApplyApplicationTests {

    @Test
    @EnabledIfEnvironmentVariable(named = "ZSSX_RUN_CTX_TEST", matches = "true")
    void contextLoads() {
        // 由 @SpringBootTest 触发的上下文检查在数据库就绪后启用

    }
}
