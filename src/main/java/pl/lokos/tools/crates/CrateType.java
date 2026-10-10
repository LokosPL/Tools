package pl.lokos.tools.crates;

import java.util.*;

public enum CrateType {
    ZWYKLA("zwykla","Zwykła","#A8A8B7","CHEST"),
    PREMIUM("premium","Premium","#FFD166","TRAPPED_CHEST"),
    AFK("afk","Aktywności","#70D6E8","BARREL"),
    EVENTOWA("eventowa","Eventowa","#89E5B0","CHEST"),
    SPECJALNA("specjalna","Specjalna","#FF727F","ENDER_CHEST");
    private final String id,title,color,block;
    CrateType(String id,String title,String color,String block){
        this.id=id;this.title=title;this.color=color;this.block=block;
    }
    public String id(){return id;}
    public String title(){return title;}
    public String color(){return color;}
    public String block(){return block;}
    public static CrateType parse(String id){
        if(id==null)return null;
        for(CrateType type:values())if(type.id.equalsIgnoreCase(id))return type;
        return null;
    }
    public static List<String> names(){return Arrays.stream(values()).map(CrateType::id).toList();}
}
