package pl.lokos.tools.config;

import pl.lokos.tools.region.*;
import java.util.*;

public final class RegionsFile {
    private ToolsConfig.Regions settings=new ToolsConfig.Regions();
    private boolean legacyImported;
    private String mainSpawn;
    private List<Region> regions=new ArrayList<>();
    public ToolsConfig.Regions settings(){return settings;}
    public boolean legacyImported(){return legacyImported;}
    public String mainSpawn(){return mainSpawn;}
    public List<Region> regions(){return List.copyOf(regions);}
    public static RegionsFile from(List<Region> regions,String main,ToolsConfig.Regions settings){
        RegionsFile file=new RegionsFile();file.settings=settings;file.regions=new ArrayList<>(regions);
        file.mainSpawn=main;file.legacyImported=true;file.validate();return file;
    }
    public void validate(){
        if(settings==null||regions==null)throw new IllegalArgumentException("Niekompletny Regions.json");
        RegionIndex index=new RegionIndex(regions);
        if(mainSpawn!=null && index.byName(mainSpawn)==null)
            throw new IllegalArgumentException("Spawn wskazuje nieistniejący region");
    }
}
