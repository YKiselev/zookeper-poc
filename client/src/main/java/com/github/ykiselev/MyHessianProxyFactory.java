package com.github.ykiselev;

import com.caucho.hessian.client.HessianProxy;
import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.io.HessianRemoteObject;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * @author Yuriy Kiselev (uze@yandex.ru).
 */
public final class MyHessianProxyFactory extends HessianProxyFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceProvider<Void> provider;

    public MyHessianProxyFactory(ServiceProvider<Void> provider) {
        this.provider = requireNonNull(provider);
    }

    @Override
    public Object create(Class<?> api, URL url, ClassLoader loader) {
        return Proxy.newProxyInstance(
                loader,
                new Class[]{api, HessianRemoteObject.class},
                new Handler(api)
        );
    }

    /**
     * Our proxy invocation handler
     */
    private final class Handler implements InvocationHandler {

        private final Class<?> api;

        private volatile MyProxy handler;

        private Handler(Class<?> api) {
            this.api = api;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            for (; ; ) {
                ServiceInstance<Void> instance = null;
                try {
                    instance = provider.getInstance();
                    if (instance == null) {
                        Thread.sleep(10);
                        continue;
                    }
                    logger.debug("Got instance: {}", instance);
                    if (instance.getUriSpec() == null) {
                        provider.noteError(instance);
                        continue;
                    }
                    final URL url = new URL(instance.buildUriSpec());
                    logger.debug("Using {}...", url);
                    if (handler != null) {
                        if (!Objects.equals(handler.getURL(), url)) {
                            handler = null;
                        }
                    }
                    if (handler == null) {
                        handler = new MyProxy(url, MyHessianProxyFactory.this, api);
                    }
                    return handler.invoke(proxy, method, args);
                } catch (InterruptedException ie) {
                    logger.warn("Interrupted: {}", ie.toString());
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.warn("Operation failed: {}", e.toString());
                    if (instance != null) {
                        provider.noteError(instance);
                    }
                    Thread.sleep(2_000);
                }
            }
            return null;
        }
    }

    /**
     * This class is needed to get access to protected ctor of {@link HessianProxy}
     */
    private final class MyProxy extends HessianProxy {

        private MyProxy(URL url, HessianProxyFactory factory, Class<?> type) {
            super(url, factory, type);
        }
    }
}
