package com.github.ykiselev;

import java.nio.charset.StandardCharsets;

/**
 * @author Yuriy Kiselev (uze@yandex.ru).
 */
public final class Bytes {

    public static byte[] toBytes(String value) {
        if (value == null) {
            return null;
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
