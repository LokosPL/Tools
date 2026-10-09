package pl.lokos.tools.region;

import java.util.UUID;

/** Zewnętrzny bufor granic; bez żadnego dodatkowego regionu lub lokalizacji. */
public final class RegionHalo {
    private RegionHalo(){}
    public static boolean contains(Region root, UUID world, int x,int z,int radius) {
        if(root==null || radius<=0 || !root.world().equals(world))return false;
        if(root.contains(world,x,z))return false;
        long minX=(long)root.minX()-radius,maxX=(long)root.maxX()+radius;
        long minZ=(long)root.minZ()-radius,maxZ=(long)root.maxZ()+radius;
        return x>=minX && x<=maxX && z>=minZ && z<=maxZ;
    }
}
