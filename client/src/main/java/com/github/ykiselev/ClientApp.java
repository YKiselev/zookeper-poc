package com.github.ykiselev;

import com.google.common.base.Stopwatch;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.DownInstancePolicy;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceProvider;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;
import org.apache.curator.x.discovery.strategies.StickyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Kiselev (uze@yandex.ru).
 */
@SpringBootApplication
@EnableScheduling
public class ClientApp {

    private static final int SERVICE_CALLS_IN_BATCH = 10_000;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean(initMethod = "start")
    CuratorFramework curator() {
        return Curator.newCuratorFramework();
    }

    @Bean(initMethod = "start")
    ServiceDiscovery<Void> serviceDiscovery() {
        return ServiceDiscoveryBuilder.builder(Void.class)
                .basePath("/services")
                .client(curator())
                .build();
    }

    @Bean(initMethod = "start")
    ServiceProvider<Void> serviceProvider() {
        return serviceDiscovery().serviceProviderBuilder()
                .serviceName(HelloService.class.getName())
                .providerStrategy(
                        new StickyStrategy<>(
                                new RoundRobinStrategy<>()
                        )
                ).downInstancePolicy(new DownInstancePolicy(30, TimeUnit.SECONDS, 1))
                .build();
    }

    @Bean
    HelloService helloService() {
        final HessianProxyFactoryBean bean = new HessianProxyFactoryBean();
        bean.setServiceInterface(HelloService.class);
        bean.setProxyFactory(
                new MyHessianProxyFactory(
                        serviceProvider()
                )
        );
        bean.setServiceUrl("http://dummy");
        bean.afterPropertiesSet();
        return (HelloService) bean.getObject();
    }

    @Scheduled(fixedDelay = 5_000, initialDelay = 1_000)
    void callService() {
        logger.info("Remote service says: {}", helloService().sayHello());
        //callServiceUsingZooKeeper();
        //callServiceDirectly();
    }

    private void callServiceUsingZooKeeper() {
        final HelloService helloService = helloService();
        final Stopwatch sw = Stopwatch.createStarted();
        long hash = 0;
        for (int i = 0; i < SERVICE_CALLS_IN_BATCH; i++) {
            hash += Objects.hashCode(helloService.sayHello());
        }
        final long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
        logger.info("ZooKeeper call speed: {} calls/sec, hash={}", (1000.0 * SERVICE_CALLS_IN_BATCH / elapsed), hash);
    }

    @Bean
    HelloService directHelloService() {
        final HessianProxyFactoryBean bean = new HessianProxyFactoryBean();
        bean.setServiceInterface(HelloService.class);
        bean.setServiceUrl("http://localhost:8080/helloService");
        bean.afterPropertiesSet();
        return (HelloService) bean.getObject();
    }

    private void callServiceDirectly() {
        final HelloService helloService = directHelloService();
        final Stopwatch sw = Stopwatch.createStarted();
        long hash = 0;
        for (int i = 0; i < SERVICE_CALLS_IN_BATCH; i++) {
            hash += Objects.hashCode(helloService.sayHello());
        }
        final long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
        logger.info("Direct call speed: {} calls/sec, hash={}", (1000.0 * SERVICE_CALLS_IN_BATCH / elapsed), hash);
    }

    public static void main(String[] args) {
        SpringApplication.run(ClientApp.class, args);
    }
}
