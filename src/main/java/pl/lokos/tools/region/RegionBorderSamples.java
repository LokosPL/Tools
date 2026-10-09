package pl.lokos.tools.region;

import java.util.ArrayList;
import java.util.List;

/** Punkty na fizycznych granicach X/Z; oblicza tylko wycinek blisko obserwatora. */
public final class RegionBorderSamples {
    public record Point(double x, double z) {}
    private RegionBorderSamples() {}

    public static List<Point> nearby(Region region, double viewerX, double viewerZ,
                                     double range, double spacing, int maxPoints) {
        if (region == null || !Double.isFinite(viewerX) || !Double.isFinite(viewerZ)
                || range <= 0 || spacing <= 0 || maxPoints <= 0) return List.of();
        List<Point> points = new ArrayList<>(Math.min(maxPoints, 160));
        double left = region.minX(), right = (double) region.maxX() + 1;
        double north = region.minZ(), south = (double) region.maxZ() + 1;

        horizontal(points, north, left, right, viewerX, viewerZ, range, spacing, maxPoints);
        horizontal(points, south, left, right, viewerX, viewerZ, range, spacing, maxPoints);
        vertical(points, left, north, south, viewerX, viewerZ, range, spacing, maxPoints);
        vertical(points, right, north, south, viewerX, viewerZ, range, spacing, maxPoints);
        return List.copyOf(points);
    }

    private static void horizontal(List<Point> out, double z, double minX, double maxX,
                                   double px, double pz, double range, double spacing, int max) {
        if (Math.abs(pz - z) > range || out.size() >= max) return;
        double first = Math.max(minX, minX + Math.ceil((px - range - minX) / spacing) * spacing);
        double last = Math.min(maxX, px + range);
        for (double x = first; x <= last + 0.0001 && out.size() < max; x += spacing) {
            if (inside(x, z, px, pz, range)) out.add(new Point(x, z));
        }
    }

    private static void vertical(List<Point> out, double x, double minZ, double maxZ,
                                 double px, double pz, double range, double spacing, int max) {
        if (Math.abs(px - x) > range || out.size() >= max) return;
        double first = Math.max(minZ, minZ + Math.ceil((pz - range - minZ) / spacing) * spacing);
        double last = Math.min(maxZ, pz + range);
        for (double z = first; z <= last + 0.0001 && out.size() < max; z += spacing) {
            if (inside(x, z, px, pz, range)) out.add(new Point(x, z));
        }
    }

    private static boolean inside(double x, double z, double px, double pz, double radius) {
        double dx = x - px, dz = z - pz;
        return dx * dx + dz * dz <= radius * radius;
    }
}
