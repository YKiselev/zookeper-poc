package com.github.ykiselev;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;

/**
 * @author Yuriy Kiselev (uze@yandex.ru).
 */
@SpringBootApplication
public class App implements Common {

    private final Logger logger = LoggerFactory.getLogger("App");

    @Bean
    CommandLineRunner runner() {
        return args -> {
            try (CuratorFramework cf = Curator.newCuratorFramework()) {
                cf.start();
                cf.blockUntilConnected();
                run(cf);
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args)
                .close();
    }

    private void run(CuratorFramework curator) throws Exception {
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
