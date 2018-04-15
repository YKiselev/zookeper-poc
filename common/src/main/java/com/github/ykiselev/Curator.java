package com.github.ykiselev;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * @author Yuriy Kiselev (uze@yandex.ru).
 */
public final class Curator implements Common {

    public static CuratorFramework newCuratorFramework() {
        return CuratorFrameworkFactory.builder()
                .connectString(CONNECT_STRING)
                .namespace("dev")
                .retryPolicy(new ExponentialBackoffRetry(1_000, 5))
                .build();
    }
}
