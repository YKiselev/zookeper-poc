package com.github.ykiselev;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
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

    public static void main(String[] args) throws Exception {
        try (CuratorFramework cf = Curator.newCuratorFramework()) {
            cf.start();
            new App().run(cf);
        }
    }

    private void run(CuratorFramework curator) throws Exception {
        //curator.delete().deletingChildrenIfNeeded().forPath("/");

        logger.info("Loading fine-grained properties...");
        final DetailedProps p1 = new DetailedProps(curator);
        p1.loadFrom(getClass().getResource("/demo.properties"));

        logger.info("Loading property files...");
        final Props p2 = new Props(curator);
        p2.loadFrom(getClass().getResource("/demo.properties"));

        printNode(curator.getZookeeperClient()
                .getZooKeeper(), "/");
    }

    private void printNode(ZooKeeper zk, String name) throws KeeperException, InterruptedException {
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
        for (String v : zk.getChildren(name, false)) {
            printNode(zk, ZKPaths.makePath(name, v));
        }
    }
}
