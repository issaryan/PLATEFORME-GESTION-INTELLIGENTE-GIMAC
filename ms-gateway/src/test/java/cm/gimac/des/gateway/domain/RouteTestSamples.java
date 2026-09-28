package cm.gimac.des.gateway.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RouteTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Route getRouteSample1() {
        return new Route()
            .id(1L)
            .path("path1")
            .targetService("targetService1")
            .requiredPermissions("requiredPermissions1")
            .rateLimitPerMinute(1);
    }

    public static Route getRouteSample2() {
        return new Route()
            .id(2L)
            .path("path2")
            .targetService("targetService2")
            .requiredPermissions("requiredPermissions2")
            .rateLimitPerMinute(2);
    }

    public static Route getRouteRandomSampleGenerator() {
        return new Route()
            .id(longCount.incrementAndGet())
            .path(UUID.randomUUID().toString())
            .targetService(UUID.randomUUID().toString())
            .requiredPermissions(UUID.randomUUID().toString())
            .rateLimitPerMinute(intCount.incrementAndGet());
    }
}
