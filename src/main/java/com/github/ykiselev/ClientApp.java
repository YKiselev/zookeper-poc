package com.github.ykiselev;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.DownInstancePolicy;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.apache.curator.x.discovery.ServiceProviderBuilder;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Kiselev (uze@yandex.ru).
 */
public final class ClientApp implements Common {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CuratorFramework curator;

    private ClientApp(CuratorFramework curator) {
        this.curator = curator;
    }

    public static void main(String[] args) throws Exception {
        try (CuratorFramework cf = App.newCuratorFramework()) {
            cf.start();
            cf.blockUntilConnected();
            new ClientApp(cf).run();
        }
    }

    private void run() throws Exception {
        logger.info("Searching for service...");
        final ServiceDiscoveryBuilder<Void> sdb = ServiceDiscoveryBuilder.builder(Void.class)
                .basePath("/services")
                .client(curator);
        try (ServiceDiscovery<Void> discovery = sdb.build()) {
            discovery.start();
            final ServiceProviderBuilder<Void> spb = discovery.serviceProviderBuilder()
                    .serviceName(HelloService.class.getName())
                    .providerStrategy(new RoundRobinStrategy<>())
                    .downInstancePolicy(new DownInstancePolicy());
            try (ServiceProvider<Void> provider = spb.build()) {
                provider.start();
                // Wait for server to be available...
                while (!Thread.currentThread().isInterrupted()) {
                    final ServiceInstance<Void> instance = provider.getInstance();
                    if (instance != null) {
                        logger.info("Got service: {}", instance);
                        break;
                    }
                }
            }
        }
        logger.info("Bye!");
    }
}
