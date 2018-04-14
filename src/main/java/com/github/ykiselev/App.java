package com.github.ykiselev;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * @author Yuriy Kiselev (uze@yandex.ru).
 */
public final class App implements Common {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CuratorFramework curator;

    private App(CuratorFramework curator) {
        this.curator = curator;
    }

    public static CuratorFramework newCuratorFramework() {
        return CuratorFrameworkFactory.builder()
                .connectString(CONNECT_STRING)
                .sessionTimeoutMs(SESSION_TIMEOUT)
                .namespace("dev")
                .retryPolicy(new ExponentialBackoffRetry(1_000, 5))
                .build();
    }

    public static void main(String[] args) throws Exception {
        try (CuratorFramework cf = newCuratorFramework()) {
            cf.start();
            cf.blockUntilConnected();
            new App(cf)
                    .run();
        }
    }

    private void run() throws Exception {
        final Stat s = curator.checkExists().forPath(PROPERTIES_ARE_LOADED);
        if (s == null) {
            logger.info("Loading properties...");
            curator.create()
                    .orSetData()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(PROPERTIES_ARE_LOADED, Bytes.toBytes("true"));

            // Load detailed properties
            final DetailedProps p1 = new DetailedProps(curator);
            p1.loadFrom(getClass().getResource("/demo.properties"));

            // Load detailed properties
            final Props p2 = new Props(curator);
            p2.loadFrom(getClass().getResource("/demo.properties"));
        } else {
            logger.info("Properties are already loaded, skipping...");
        }
        // Show properties
        printTree(curator.getZookeeperClient()
                .getZooKeeper());
        logger.info("Bye!");
    }

    private void printTree(ZooKeeper zk) {
        printNode(zk, "/");
    }

    private void printNode(ZooKeeper zk, String name) {
        try {
            final Stat stat = zk.exists(name, false);
            if (stat == null) {
                return;
            }
            if (stat.getDataLength() > 0) {
                final byte[] data = zk.getData(name, false, null);
                logger.info("{} = {}", name, new String(data, StandardCharsets.UTF_8));
            } else {
                logger.info(name);
            }
            zk.getChildren(name, false)
                    .forEach(v -> printNode(zk, ZKPaths.makePath(name, v)));
        } catch (KeeperException e) {
            logger.error("Tree traversal failed!", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
