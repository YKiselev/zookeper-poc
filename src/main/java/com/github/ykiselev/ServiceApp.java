package com.github.ykiselev;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * @author Yuriy Kiselev (uze@yandex.ru).
 */
public final class ServiceApp implements Common {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CuratorFramework curator;

    private ServiceApp(CuratorFramework curator) {
        this.curator = curator;
    }

    public static void main(String[] args) throws Exception {
        try (CuratorFramework cf = App.newCuratorFramework()) {
            cf.start();
            cf.blockUntilConnected();
            new ServiceApp(cf).run();
        }
    }

    private void run() throws Exception {
        logger.info("Registering service...");
        final ServiceDiscoveryBuilder<Void> sdb = ServiceDiscoveryBuilder.builder(Void.class)
                .basePath("/services")
                .client(curator)
                .thisInstance(
                        ServiceInstance.<Void>builder()
                                .name(HelloService.class.getName())
                                .address("localhost")
                                .port(51001)
                                .enabled(true)
                                .id(UUID.randomUUID().toString())
                                .serviceType(ServiceType.DYNAMIC) // ????
                                .build()
                );
        try (ServiceDiscovery<Void> discovery = sdb.build()) {
            discovery.start();
            discovery.queryForNames().forEach(v -> {
                try {
                    discovery.queryForInstances(v).forEach(s -> logger.info("  {} : {}", v, s));
                } catch (Exception e) {
                    logger.error("Operation failed!", e);
                }
            });

//            final ServiceProviderBuilder<Void> spb = discovery.serviceProviderBuilder()
//                    .serviceName(HelloService.class.getName())
//                    .providerStrategy(new RoundRobinStrategy<>())
//                    .downInstancePolicy(new DownInstancePolicy());
            //try (ServiceProvider<Void> serviceProvider = spb.build()) {
            //serviceProvider.start();
            //logger.info("Service registered: {}", serviceProvider.getInstance());
            logger.info("Entering main loop...");
            // main loop
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(10);
            }
            //}
        }
        logger.info("Bye!");
    }
}
