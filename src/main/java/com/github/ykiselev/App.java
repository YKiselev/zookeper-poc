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
public final class App {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CuratorFramework curator;

    private App(CuratorFramework curator) {
        this.curator = curator;
    }

    public static void main(String[] args) throws Exception {
        final CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString("localhost:2181")
                .connectionTimeoutMs(5_000)
                .namespace("test-app")
                .retryPolicy(new ExponentialBackoffRetry(1_000, 5));
        try (CuratorFramework cf = builder.build()) {
            cf.start();
            cf.blockUntilConnected();
            new App(cf)
                    .run();
        }
    }

    private void run() throws Exception {
        // 1
        final String areLoaded = "/properties/areLoaded";
        final Stat s = curator.checkExists().forPath(areLoaded);
        if (s == null) {
            logger.info("Loading properties...");
            curator.create()
                    .orSetData()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(areLoaded, "true".getBytes());
            //.creatingParentsIfNeeded()
            //.forPath(ZKPaths.makePath(areLoaded, "foo"));
            //curator.setData().forPath(areLoaded, "true".getBytes());
        } else {
            logger.info("Properties are already loaded, skipping...");
        }
        // 2
        final Props props = new Props(curator);
        props.loadFrom(getClass().getResource("/demo.properties"));
        // 3
        printTree(curator.getZookeeperClient()
                .getZooKeeper());
        // 4
        while (!Thread.currentThread().isInterrupted()) {
            Thread.sleep(10);
        }
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
