package com.wechatapply.associationwechatapply.common.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * SHA-256 哈希工具单元测试（SRS 5.4 归档固证）。
 */
class HashUtilTest {

    @Test
    void sha256OfKnownValue() {
        // "abc" 的 SHA-256 标准值
        String hex = HashUtil.sha256Hex("abc".getBytes(StandardCharsets.UTF_8));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hex);
    }

    @Test
    void streamAndBytesAgree() {
        byte[] data = "中山商协会档案固证内容".getBytes(StandardCharsets.UTF_8);
        assertEquals(HashUtil.sha256Hex(data),
                HashUtil.sha256Hex(new ByteArrayInputStream(data)));
    }

    @Test
    void differentInputsDifferentHash() {
        assertNotEquals(HashUtil.sha256Hex("a".getBytes()), HashUtil.sha256Hex("b".getBytes()));
    }
}
