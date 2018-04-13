package com.github.ykiselev;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

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
public final class Props {

    private final CuratorFramework curator;

    public Props(CuratorFramework curator) {
        this.curator = requireNonNull(curator);
    }

    public void loadFrom(URL res) {
        try (InputStream is = res.openStream()) {
            final Properties p = new Properties();
            p.load(is);
            p.forEach((k, v) -> set((String) k, (String) v));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void set(String name, String value) {
        if (name == null) {
            return;
        }
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        try {
            curator.checkExists()
                    .creatingParentsIfNeeded()
                    .forPath(ZKPaths.makePath(name, "foo"));
            curator.setData()
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
