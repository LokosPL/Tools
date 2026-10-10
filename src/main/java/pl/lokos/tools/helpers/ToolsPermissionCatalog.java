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
        add(list,"tools.msg.staff","Wiadomości do administracji",
                "Umożliwia pisanie prywatnych wiadomości do chronionej administracji.",
                "Bez tej permisji zwykli gracze mogą wysyłać MSG tylko do niechronionych odbiorców.");
        add(list,"tools.msg.protected","Chroniona skrzynka MSG",
                "Blokuje prywatne wiadomości od graczy bez tools.msg.staff.",
                "Rangi administracyjne są chronione automatycznie, nawet bez tego uprawnienia.");
        add(list,"tools.msg.bypass.cooldown","Bez limitu MSG",
                "Omija odstęp czasowy pomiędzy prywatnymi wiadomościami.",
                "Nie omija wyciszenia ani wyłączenia prywatnych wiadomości.");
        add(list,commands.chat().permission(),"Zarządzanie czatem",
                "Zmiana stanu czatu, czyszczenie, wyciszenia i dostęp rang.",
                "Komenda /chat z pełną kontrolą moderacji.");
        add(list,"tools.chat.bypass.slow","Bez limitu wiadomości",
                "Nie podlega ograniczeniu 2 wiadomości na 3 sekundy.",
                "Przydatne dla moderatorów i rang specjalnych.");
        add(list,"tools.chat.bypass.lock","Pisanie przy zamkniętym czacie",
                "Omija wyłączenie czatu i ograniczenie według rangi.",
                "Nie omija wyciszenia ani antyspamu bez dodatkowych permisji.");
        add(list,"tools.chat.bypass.mute","Omijanie wyciszeń",
                "Może pisać mimo indywidualnego wyciszenia.",
                "Nadawaj wyłącznie zaufanej moderacji.");
        add(list,commands.tp().permission(),"Teleportowanie siebie","/tp nick lub /tp x y z","Dostęp administracyjny.");
        add(list,"tools.tp.others","Teleportowanie innych graczy","Teleportacja wskazanego gracza na współrzędne","Wymaga także tools.tp.");
        add(list,"tools.tp.all","Teleportacja wszystkich","/tp * i /tp * x y z","Wymaga także tools.tp.");
        add(list,commands.vanish().permission(),"Włączanie vanish","Niewidzialność dla zwykłych graczy","Stan zachowuje się po wyjściu.");
        add(list,"tools.vanish.see","Widzenie vanisha","Dostęp do ukrytych graczy i etykiety VANISH","Tylko moderacja.");
        add(list,"tools.vanish.monitor","Powiadomienia vanish","Chatowe alerty o przełączeniach","Tylko wyższe rangi.");
        add(list,"tools.vanish.others","Vanish innych","/vanish nick [wlacz|wylacz]","Wymaga tools.vanish.use.");
        add(list,"tools.helpop.receive","Odbieranie helpop","Prywatne zgłoszenia graczy","Tylko kadra.");
        add(list,"tools.helpop.bypass.cooldown","Helpop bez limitu","Omijanie antyspamu /helpop","Tylko kadra.");
        add(list,commands.gamemode().permission(),"Zmiana trybu gry","/gm i /gamemode","Tryby 1-4.");
        add(list,"tools.gamemode.others","Tryb gry innych","Zmiana trybu gry wskazanego gracza","Wymaga tools.gamemode.");
        add(list,commands.fly().permission(),"Latanie","/fly wlacz/wylacz","Dla własnej postaci.");
        add(list,"tools.fly.others","Latanie innych","Przełączanie możliwości latania innych graczy","Wymaga tools.fly.");
        add(list,"tools.fly.monitor","Powiadomienia fly","Alerty o włączeniu latania","Tylko kadra.");
        add(list,commands.broadcast().permission(),"Ogłoszenia bossbar","/broadcast 30s tekst","Działa również po restarcie.");
        add(list,commands.inventoryopen().permission(),"Podgląd eq","/inventoryopen eq nick","Tylko uprawnione rangi.");
        add(list,"tools.inventoryopen.enderchest","Podgląd enderchesta","/inventoryopen enderchest nick","Wymaga tools.inventoryopen.");
        add(list,commands.speed().permission(),"Prędkość ruchu","Zmiana szybkości chodzenia lub latania","Chodzenie i latanie.");
        add(list,"tools.speed.others","Prędkość innych","Zmiana szybkości innego gracza","Wymaga tools.speed.");
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
                commands.tools().permission(),"tools.whitelist.admin",commands.chat().permission(),
                commands.tp().permission(),commands.vanish().permission(),
                commands.gamemode().permission(),commands.fly().permission(),
                commands.broadcast().permission(),commands.inventoryopen().permission(),
                commands.speed().permission());
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
