package com.github.ykiselev;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * @author Yuriy Kiselev (uze@yandex.ru).
 */
public final class DetailedProps {

    private final CuratorFramework curator;

    public DetailedProps(CuratorFramework curator) {
        this.curator = requireNonNull(curator);
    }

    public void loadFrom(URL res) {
        try (InputStream is = res.openStream()) {
            final Properties p = new Properties();
            p.load(is);
            p.forEach((k, v) -> set("/" + k, (String) v));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void set(String name, String value) {
        try {
            curator.create()
                    .orSetData()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(name, raw(value));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] raw(String value) {
        if (value == null) {
            return null;
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
