package com.github.ykiselev;

import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * @author Yuriy Kiselev (uze@yandex.ru).
 */
public final class Props {

    private final ZooKeeper zk;

    public Props(ZooKeeper zk) {
        this.zk = requireNonNull(zk);
    }

    public void loadFrom(URL res) {
        try (InputStream is = res.openStream()) {

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
