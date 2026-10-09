package pl.lokos.tools.region;

import java.util.UUID;

/** Polityka przy zmianach pomiedzy obszarami: wymagana ochrona celu i zrodla. */
public final class RegionPolicy {
    private RegionPolicy() {}
    public static boolean mayMove(RegionIndex index, UUID world, int fromX,int fromZ,int toX,int toZ,
                                  RegionFlag flag) {
        Region source=index.at(world,fromX,fromZ);
        Region destination=index.at(world,toX,toZ);
        return (source==null || index.enabled(source,flag))
                && (destination==null || index.enabled(destination,flag));
    }
}
