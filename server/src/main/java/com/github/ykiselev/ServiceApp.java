package com.github.ykiselev;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceType;
import org.apache.curator.x.discovery.UriSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.remoting.caucho.HessianServiceExporter;

import java.nio.BufferUnderflowException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Yuriy Kiselev (uze@yandex.ru).
 */
@SpringBootApplication
public class ServiceApp {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${server.port}")
    private int webPort;

    @Value("${failThreshold}")
    private double failThreshold;

    @Bean(initMethod = "start")
    CuratorFramework curator() {
        return Curator.newCuratorFramework();
    }

    @Bean(initMethod = "start")
    ServiceDiscovery<Void> discovery() throws Exception {
        logger.info("Registering service...");
        final ServiceDiscoveryBuilder<Void> sdb = ServiceDiscoveryBuilder.builder(Void.class)
                .basePath("/services")
                .client(curator());
//                .thisInstance(
//                        ServiceInstance.<Void>builder()
//                                .name(HelloService.class.getName())
//                                .address("localhost")
//                                .port(webPort)
//                                .id(UUID.randomUUID().toString())
//                                .serviceType(ServiceType.DYNAMIC) // ????
//                                .uriSpec(new UriSpec("{scheme}://{adddress}:{port}/helloService"))
//                                .build()
//                );
        return sdb.build();
    }

    @Bean
    CommandLineRunner runner() {
        return args -> {
            final ServiceDiscovery<Void> discovery = discovery();
            discovery.registerService(
                    ServiceInstance.<Void>builder()
                            .name(HelloService.class.getName())
                            .address("localhost")
                            .port(webPort)
                            .id(UUID.randomUUID().toString())
                            .serviceType(ServiceType.DYNAMIC)
                            .uriSpec(new UriSpec("{scheme}://{address}:{port}/helloService"))
                            .build()
            );
            discovery.queryForNames().forEach(v -> {
                try {
                    discovery.queryForInstances(v)
                            .forEach(s -> logger.info("  {} : {}", v, s));
                } catch (Exception e) {
                    logger.error("Operation failed!", e);
                }
            });
        };
    }

    @Bean
    HelloService helloService() {
        return () -> {
            if (ThreadLocalRandom.current().nextDouble() >= failThreshold) {
                throw new BufferUnderflowException();
            }
            return "Hello @ " + new Date();
        };
    }

    @Bean(name = "/helloService")
    HessianServiceExporter exporter() {
        final HessianServiceExporter exporter = new HessianServiceExporter();
        exporter.setServiceInterface(HelloService.class);
        exporter.setService(helloService());
        return exporter;
    }


    public static void main(String[] args) {
        SpringApplication.run(ServiceApp.class, args);
    }
}
