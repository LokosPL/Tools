package pl.lokos.tools.region;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RegionBorderSamplesTest {
    private final UUID world = UUID.randomUUID();

    @Test
    void pointsAreDenseNearTheObserverRegardlessOfHugeRegionSize() {
        Region region = new Region("spawn", world, -1000, 1000, -1000, 1000,
                null, null, Map.of(), null);
        var points = RegionBorderSamples.nearby(region, 999, 0, 36, 1.5, 150);
        assertTrue(points.size() >= 40, "Gracz przy granicy powinien widziec gesta linie");
        assertTrue(points.size() <= 150);
        assertTrue(points.stream().allMatch(p ->
                p.x() == 1001 || p.x() == -1000 || p.z() == -1000 || p.z() == 1001));
        assertTrue(points.stream().allMatch(p ->
                Math.hypot(p.x() - 999, p.z()) <= 36 + 0.0001));
        double lastZ = Double.NaN;
        for (var point : points) {
            if (point.x() == 1001) {
                if (!Double.isNaN(lastZ)) assertTrue(point.z() - lastZ <= 1.5001);
                lastZ = point.z();
            }
        }
    }

    @Test
    void distantEdgesAreNotRenderedOrTraversed() {
        Region region = new Region("large", world, -30000, 30000, -30000, 30000,
                null, null, Map.of(), null);
        assertTrue(RegionBorderSamples.nearby(region, 0, 0, 36, 1.5, 150).isEmpty());
        assertTrue(RegionBorderSamples.nearby(region, 30000, 0, 36, 1.5, 150).size() <= 150);
    }

    @Test
    void nearCornersRenderBothAxesWithoutThousandsOfPoints() {
        Region region = new Region("arena", world, 0, 40, 0, 40,
                null, null, Map.of(), null);
        var samples = RegionBorderSamples.nearby(region, 1, 1, 36, 1.5, 140);
        assertTrue(samples.stream().anyMatch(p -> p.x() == 0));
        assertTrue(samples.stream().anyMatch(p -> p.z() == 0));
        assertTrue(samples.size() <= 140);
    }
}
