package pl.lokos.tools.crates;

import java.util.UUID;

/** Weryfikacja fizycznej skrzyni przy finalizacji GUI bez zależności od Bukkit. */
public final class CrateOpeningPolicy {
    private CrateOpeningPolicy() {}
    public static boolean near(CratesState.Position position,UUID world,double x,double y,double z){
        if(position==null||world==null||!position.world().equals(world.toString()))return false;
        double dx=x-position.x()-0.5d;
        double dy=y-position.y()-0.5d;
        double dz=z-position.z()-0.5d;
        return Double.isFinite(dx)&&Double.isFinite(dy)&&Double.isFinite(dz)
                && dx*dx+dy*dy+dz*dz<=36d;
    }
}
