package org.example.nongye.util;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 自动探测本机局域网 IP（用于生成手机可访问的二维码地址）
 * 无需配置，换 WiFi / 热点后自动跟随当前网络。
 */
public class LanIpUtil {

    /**
     * 获取当前出口网卡对应的局域网地址，如 172.16.57.215
     * 原理：创建一个到公网地址的 UDP "连接"（不实际发包），
     * 操作系统会自动选择默认路由网卡，取其本地地址即为当前出口 IP。
     */
    public static String getLanIp() {
        // 方案一：默认路由探测（最准）
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10053);
            String ip = socket.getLocalAddress().getHostAddress();
            if (isValid(ip)) {
                return ip;
            }
        } catch (Exception ignore) {
        }
        // 方案二：枚举网卡，取第一个合法的局域网地址
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    String ip = addrs.nextElement().getHostAddress();
                    if (isValid(ip)) {
                        return ip;
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /**
     * 获取完整基础地址，如 http://172.16.57.215:8080；探测失败返回 fallback
     */
    public static String getLanUrl(int port, String fallback) {
        String ip = getLanIp();
        if (ip == null) {
            return fallback;
        }
        return "http://" + ip + ":" + port;
    }

    private static boolean isValid(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        // 排除回环、IPv6、虚拟网卡常见网段由调用处网卡过滤兜底
        return !ip.startsWith("127.") && !ip.contains(":") && ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
    }
}
