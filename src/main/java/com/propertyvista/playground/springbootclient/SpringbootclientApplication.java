package com.propertyvista.playground.springbootclient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class SpringbootclientApplication {

    private enum OperationType {
        cpu_consuming("/primes?value=100000"), //
        time_consuming("/timeConsumingOperation?value=1000"); // 1 sec. aprox.

        String path;

        OperationType(String path) {
            this.path = path;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(SpringbootclientApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringbootclientApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

//    @Bean
//    public CommandLineRunner run(RestTemplate restTemplate) throws Exception {
//        return args -> {
//
//            ExecutorService executorService = Executors.newFixedThreadPool(10); // starts 10 requests at a time
//
//            long startTime = System.currentTimeMillis();
//
//            for (int i = 0; i < 10; i++) {
//                Future<String> result = executorService.submit(createRequest(OperationType.cpu_consuming));
//                log.info(result.get());
//            }
//
//            executorService.shutdown();
//
//            // Wait till test is done
//            while (!executorService.isTerminated()) {
//                try {
//                    Thread.sleep(5);
//                } catch (InterruptedException e) {
//                    log.error("Error", e);
//                }
//            }
//
//            log.info("Total time: " + toSec(since(startTime)));
//            System.exit(0);
//        };
//    }

    @Bean
    public CommandLineRunner run(RestTemplate restTemplate) throws Exception {
        return args -> {

            ExecutorService executorService = Executors.newFixedThreadPool(10); // starts 10 requests at a time

            List<Callable<String>> requests = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                requests.add(createRequest(OperationType.cpu_consuming));
            }

            long startTime = System.currentTimeMillis();

            try {
                executorService.invokeAll(requests).stream().map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }).forEach(log::info);
            } catch (InterruptedException e) {
                log.error("Error", e);
            }

            executorService.shutdown();

            try {
                if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }

            log.info("Total time: " + toSec(since(startTime)));
            System.exit(0);
        };
    }

    private static Callable<String> createRequest(OperationType operationType) {
        return () -> {
            String result = "";
            try {
                result = new RestTemplate().getForObject("http://localhost:4000" + operationType.path, String.class);
            } catch (Throwable e) {
                log.error("Error ", e);
            }
            return result;
        };
    }

    private static String toSec(long time) {
        return String.format("%10.4f seconds", ((double) time / 1000));
    }

    public static long since(long start) {
        if (start == 0) {
            return 0;
        }
        return (System.currentTimeMillis() - start);
    }

}
