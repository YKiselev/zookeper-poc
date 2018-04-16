package com.github.ykiselev;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * @author Yuriy Kiselev (uze@yandex.ru).
 */
public final class Curator {

    private static final String CONNECT_STRING = "localhost:2181";

    public static CuratorFramework newCuratorFramework() {
        return CuratorFrameworkFactory.builder()
                .connectString(System.getProperty("zk.connectString", CONNECT_STRING))
                .namespace("dev")
                .retryPolicy(new ExponentialBackoffRetry(1_000, 5))
                .build();
    }
}
