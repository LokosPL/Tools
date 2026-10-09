package pl.lokos.tools.config;

import pl.lokos.tools.manager.RankSnapshot;
import java.util.*;

public final class RanksFile {
    private ToolsConfig.Ranks settings=new ToolsConfig.Ranks();
    private Map<String,RankEntry> ranks=new LinkedHashMap<>();
    public record RankEntry(String prefix,String suffix,Integer position,String joinMessage,Set<String> permissions) {
        public RankEntry {
            if(prefix==null || suffix==null || joinMessage==null || permissions==null)throw new IllegalArgumentException("Niepełna ranga");
            permissions=Set.copyOf(permissions);
        }
        public RankSnapshot.Rank toRank(String name){
            return new RankSnapshot.Rank(name,prefix,suffix,position,joinMessage);
        }
    }
    public ToolsConfig.Ranks settings(){return settings;}
    public Map<String,RankEntry> ranks(){return Map.copyOf(ranks);}
    public static RanksFile from(Map<String,RankEntry> ranks,ToolsConfig.Ranks settings){
        RanksFile file=new RanksFile();
        file.settings=settings;
        file.ranks=new LinkedHashMap<>(ranks);
        file.validate();
        return file;
    }
    public void validate(){
        if(settings==null||ranks==null)throw new IllegalArgumentException("Niekompletny Ranks.json");
        for(var item:ranks.entrySet()){
            if(!item.getKey().matches("[\\p{L}0-9_-]{1,24}")||item.getValue()==null)
                throw new IllegalArgumentException("Błędna definicja rangi: "+item.getKey());
        }
    }
}
