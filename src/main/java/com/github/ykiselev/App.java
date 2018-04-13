package com.github.ykiselev;

import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Kiselev (uze@yandex.ru).
 */
public final class App implements Watcher {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CuratorZookeeperClient client;

    private App(CuratorZookeeperClient client) {
        this.client = client;
    }

    public static void main(String[] args) throws Exception {
        final CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(5_000)
                .namespace("test-app")
                .retryPolicy(new ExponentialBackoffRetry(1_000, 5))
                .build();
        curatorFramework.blockUntilConnected();
        new App(curatorFramework.getZookeeperClient())
                .run();
    }

    private void run() throws Exception {
        try {
            final String path = "/a/b";
            final Stat stat = zk.exists(path, false);
            if (stat == null) {
                logger.info("No value found, creating...");
                final String v = zk.create(
                        path,
                        new byte[]{1, 2, 3},
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );
                logger.info("Created: {}", v);
            } else {
                logger.info("Value already exists!");
            }
            while (!Thread.currentThread().isInterrupted()) {
                final ZooKeeper.States state = zk.getState();
                if (!state.isAlive()) {
                    break;
                }
                Thread.sleep(10);
            }
        } finally {
            zk.close();
        }
        logger.info("Bye!");
    }

    @Override
    public void process(WatchedEvent event) {
        logger.info("Got event: {}", event);
    }

    private void ensureConnected() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            final ZooKeeper.States state = zk.getState();
            if (state.isConnected()) {
                break;
            } else if (!state.isAlive()) {
                throw new IllegalStateException("Not alive!");
            }
            Thread.sleep(10);
        }
    }

    private byte[] tryGet(String name) {
        try {
            final Stat stat = zk.exists(name, false);
            if (stat != null) {
                return zk.getData(name, null, null);
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted: {}", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Operation failed!", e);
        }
        return null;
    }
}
