package pl.lokos.tools.config;

import java.util.*;

/** Kazda komenda ma identyczny zestaw opcji; tylko Java rejestruje komendy. */
public final class CommandsFile {
    private String serverName="TOOLS";
    private String messagePrefix="";
    public String serverName(){return serverName;}
    public String messagePrefix(){return messagePrefix.replace("{server}",serverName);}

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
    private Entry chat=new Entry(true,"Zarządzanie czatem",List.of("czat"),"tools.chat.admin");
    private Entry msg=new Entry(true,"Prywatne wiadomości",List.of("tell","w"),"tools.msg.use");
    private Entry reply=new Entry(true,"Odpowiedź na prywatną wiadomość",List.of("r","replay"),"tools.msg.use");
    public Entry tools(){return tools;}
    public Entry ranga(){return ranga;}
    public Entry region(){return region;}
    public Entry lokalizacje(){return lokalizacje;}
    public Entry chat(){return chat;}
    public Entry msg(){return msg;}
    public Entry reply(){return reply;}
    public void validate(){
        if(serverName==null||serverName.isBlank()||serverName.length()>32)
            throw new IllegalArgumentException("serverName powinno mieć 1-32 znaki");
        if(messagePrefix==null||messagePrefix.length()>128)
            throw new IllegalArgumentException("Niepoprawne messagePrefix");
        if(tools==null||ranga==null||region==null||lokalizacje==null)throw new IllegalArgumentException("Niekompletne Commands.json");
        tools.validate("tools");ranga.validate("ranga");region.validate("region");lokalizacje.validate("lokalizacje");
        if(chat==null)throw new IllegalArgumentException("Brakuje Commands.chat");
        chat.validate("chat");
        if(msg==null || reply==null)throw new IllegalArgumentException("Brak konfiguracji msg/reply.");
        msg.validate("msg");reply.validate("reply");
        Set<String> used=new HashSet<>(Set.of("tools","ranga","region","lokalizacje","chat","msg","reply"));
        for(Entry entry:List.of(tools,ranga,region,lokalizacje,chat,msg,reply))for(String alias:entry.aliases()){
            if(!used.add(alias.toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Powtórzony alias komendy: "+alias);
        }
    }
}
