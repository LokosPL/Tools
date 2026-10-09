package pl.lokos.tools.config;

import java.util.*;

/** Kazda komenda ma identyczny zestaw opcji; tylko Java rejestruje komendy. */
public final class CommandsFile {
    public static final class Entry {
        private boolean enabled;
        private String description;
        private List<String> aliases;
        private String permission;
        public Entry() {}
        public Entry(boolean enabled,String description,List<String> aliases,String permission) {
            this.enabled=enabled;this.description=description;this.aliases=List.copyOf(aliases);this.permission=permission;
        }
        public boolean enabled(){return enabled;}
        public String description(){return description;}
        public List<String> aliases(){return List.copyOf(aliases);}
        public String permission(){return permission;}
        public void validate(String name){
            if(description==null || description.isBlank())throw new IllegalArgumentException("Brak opisu "+name);
            if(permission==null || !permission.matches("[a-z0-9_.-]+"))throw new IllegalArgumentException("Błędne permission "+name);
            if(aliases==null)throw new IllegalArgumentException("Brak aliases "+name);
            for(String alias:aliases)
                if(alias==null || !alias.matches("[a-zA-Z0-9_-]+"))throw new IllegalArgumentException("Błędny alias "+name);
        }
    }
    private Entry tools=new Entry(true,"Narzędzia administracyjne",List.of(),"tools.admin");
    private Entry ranga=new Entry(true,"Zarządzanie rangami",List.of(),"tools.ranga.admin");
    private Entry region=new Entry(true,"Zarządzanie regionami",List.of(),"tools.region.admin");
    private Entry lokalizacje=new Entry(true,"Teleportacja do lokalizacji",List.of("lokacje"),"tools.lokalizacje");
    public Entry tools(){return tools;}
    public Entry ranga(){return ranga;}
    public Entry region(){return region;}
    public Entry lokalizacje(){return lokalizacje;}
    public void validate(){
        if(tools==null||ranga==null||region==null||lokalizacje==null)throw new IllegalArgumentException("Niekompletne Commands.json");
        tools.validate("tools");ranga.validate("ranga");region.validate("region");lokalizacje.validate("lokalizacje");
        Set<String> used=new HashSet<>(Set.of("tools","ranga","region","lokalizacje"));
        for(Entry entry:List.of(tools,ranga,region,lokalizacje))for(String alias:entry.aliases()){
            if(!used.add(alias.toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Powtórzony alias komendy: "+alias);
        }
    }
}
