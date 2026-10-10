package pl.lokos.tools.helpers;

import pl.lokos.tools.config.CommandsFile;
import java.util.*;

/**
 * Jawna lista TYLKO funkcji Tools. Żadne Bukkit/vanilla/inne pluginy nie są
 * automatycznie dodawane do panelu ani podpowiedzi komend.
 */
public final class ToolsPermissionCatalog {
    public record Feature(String permission,String name,String description,String details) {}

    private final List<Feature> features;
    private final Set<String> nodes;
    private final Set<String> administrative;

    public ToolsPermissionCatalog(CommandsFile commands) {
        List<Feature> list=new ArrayList<>();
        add(list,commands.lokalizacje().permission(),"Menu lokalizacji",
                "Otwieranie GUI pod /lokalizacje.",
                "Bez tego uprawnienia zwykły gracz nie otworzy menu ani nie zobaczy komendy po /.");
        add(list,"tools.lokalizacje.instant","Natychmiastowa teleportacja",
                "Teleportacja bez odliczania.",
                "Dodatkowy dostęp bez 5 sekund czekania; aktywne rangi mają go także z tytułu nadania.");
        add(list,commands.region().permission(),"Administracja regionami",
                "Tworzenie, usuwanie i edycja regionów.",
                "Udostępnia /region, różdżkę, zmianę zasad i dostęp do panelu ochrony.");
        add(list,"tools.spawn","Teleportacja na spawn",
                "Standardowe przeniesienie na główną lokalizację serwera.",
                "Bez uprawnień administracyjnych.");
        add(list,commands.ranga().permission(),"Zarządzanie rangami",
                "Dostęp do /ranga i GUI uprawnień.",
                "Umożliwia tworzenie rang, nadawanie, ustawianie pozycji oraz edycję.");
        add(list,"tools.whitelist.admin","Zarządzanie whitelistą",
                "Panel i komendy whitelisty.",
                "Włączanie trybów przerwy, dodawanie i usuwanie graczy.");
        add(list,commands.tools().permission(),"Narzędzia administracyjne",
                "Dostęp do /tools.",
                "Status serwera, test połączenia MySQL i statystyki.");
        add(list,"*","Pełne uprawnienia",
                "Wszystkie uprawnienia i status operatora.",
                "Przyznaje pełny dostęp serwerowy; używaj tylko przy rangach administracyjnych.");
        features=List.copyOf(list);
        Set<String> names=new LinkedHashSet<>();
        for(Feature feature:features)if(!feature.permission().equals("*"))names.add(feature.permission());
        nodes=Set.copyOf(names);
        administrative=Set.of(commands.region().permission(),commands.ranga().permission(),
                commands.tools().permission(),"tools.whitelist.admin");
    }

    private static void add(List<Feature> list,String node,String name,String description,String details) {
        if(node==null||node.isBlank())throw new IllegalArgumentException("Brakuje nazwy uprawnienia.");
        if(list.stream().noneMatch(feature->feature.permission().equalsIgnoreCase(node)))
            list.add(new Feature(node,name,description,details));
    }

    public List<Feature> features(){return features;}
    public Set<String> managedNodes(){return nodes;}
    public Set<String> administrativeNodes(){return administrative;}
    public List<String> suggestions(){return features.stream().map(Feature::permission).toList();}
    public boolean isManaged(String permission){
        return permission!=null && (permission.equals("*")||nodes.contains(permission));
    }

    /** Sprawdza tylko wirtualne uprawnienia Tools, nie domyślne uprawnienia Bukkit. */
    public static boolean granted(Set<String> effectiveRankPermissions,boolean operator,String node) {
        return operator || (effectiveRankPermissions!=null
                && (effectiveRankPermissions.contains("*")||effectiveRankPermissions.contains(node)));
    }
}
