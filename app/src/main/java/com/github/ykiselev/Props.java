package com.github.ykiselev;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;

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
            set("/" + FilenameUtils.getName(res.getFile()),
                    IOUtils.toByteArray(is));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void set(String name, byte[] value) {
        try {
            curator.create()
                    .orSetData()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(name, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
