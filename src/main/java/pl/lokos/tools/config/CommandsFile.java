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
    private Entry tp=new Entry(true,"Teleportacja administracyjna",List.of(),"tools.tp");
    private Entry vanish=new Entry(true,"Tryb niewidzialności administracji",List.of(),"tools.vanish.use");
    private Entry helpop=new Entry(true,"Kontakt z administracją",List.of(),"tools.helpop.use");
    private Entry gamemode=new Entry(true,"Zmiana trybu gry",List.of("gm"),"tools.gamemode");
    private Entry fly=new Entry(true,"Zarządzanie lataniem",List.of(),"tools.fly");
    private Entry broadcast=new Entry(true,"Ogłoszenia bossbar",List.of(),"tools.broadcast");
    private Entry inventoryopen=new Entry(true,"Podgląd ekwipunków",List.of("invopen"),"tools.inventoryopen");
    private Entry speed=new Entry(true,"Zmiana prędkości gracza",List.of(),"tools.speed");
    public Entry tools(){return tools;}
    public Entry ranga(){return ranga;}
    public Entry region(){return region;}
    public Entry lokalizacje(){return lokalizacje;}
    public Entry chat(){return chat;}
    public Entry msg(){return msg;}
    public Entry reply(){return reply;}
    public Entry tp(){return tp;}
    public Entry vanish(){return vanish;}
    public Entry helpop(){return helpop;}
    public Entry gamemode(){return gamemode;}
    public Entry fly(){return fly;}
    public Entry broadcast(){return broadcast;}
    public Entry inventoryopen(){return inventoryopen;}
    public Entry speed(){return speed;}
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
        if(tp==null||vanish==null||helpop==null||gamemode==null||fly==null||broadcast==null
                ||inventoryopen==null||speed==null)
            throw new IllegalArgumentException("Brakuje konfiguracji komend administracyjnych.");
        tp.validate("tp");vanish.validate("vanish");helpop.validate("helpop");
        gamemode.validate("gamemode");fly.validate("fly");broadcast.validate("broadcast");
        inventoryopen.validate("inventoryopen");speed.validate("speed");
        Set<String> used=new HashSet<>(Set.of("tools","ranga","region","lokalizacje","chat","msg","reply",
                "tp","vanish","helpop","gamemode","fly","broadcast","inventoryopen","speed"));
        for(Entry entry:List.of(tools,ranga,region,lokalizacje,chat,msg,reply,
                tp,vanish,helpop,gamemode,fly,broadcast,inventoryopen,speed))for(String alias:entry.aliases()){
            if(!used.add(alias.toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Powtórzony alias komendy: "+alias);
        }
    }
}
