package org.example.nongye.util;

import org.example.nongye.util.QRCodeUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 二维码生成工具测试
 */
class QRCodeUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void generateQRCodeCreatesValidPng() throws Exception {
        Path out = tempDir.resolve("qr.png");
        QRCodeUtil.generateQRCode("http://localhost:8080/trace/TZ123", out.toString(), 200, 200);
        assertTrue(Files.exists(out), "二维码文件应已生成");
        assertTrue(Files.size(out) > 0, "文件不应为空");
        // PNG 文件头签名
        byte[] head = Files.readAllBytes(out);
        assertEquals((byte) 0x89, head[0], "应为 PNG 文件");
    }

    @Test
    void generateQRCodeThrowsOnInvalidPath() {
        // 向不存在的目录写入应抛异常
        boolean thrown = false;
        try {
            QRCodeUtil.generateQRCode("content", "/nonexistent-dir-xyz/qr.png", 100, 100);
        } catch (Exception expected) {
            thrown = true;
        }
        assertTrue(thrown, "应抛出异常");
    }
}
