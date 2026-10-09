package pl.lokos.tools.region;

import java.util.Arrays;
import java.util.Locale;

/** Szczegółowe zasady; stara nazwa flagi pozostaje zgodna z Regions.json. */
public enum RegionFlag {
    BUILD("budowanie","Bloki"), BREAK("niszczenie","Bloki"),
    PVP("pvp","Walka"), DAMAGE("obrazenia","Walka"), MOBS("moby","Walka"),
    EXPLOSIONS("wybuchy","Środowisko"), FLUIDS("plyny","Środowisko"),
    PISTONS("tloki","Środowisko"), FIRE("ogien","Środowisko"),
    INTERACT("interakcje","Interakcje"),
    CHESTS("skrzynie","Interakcje"), CRAFTING("crafting","Interakcje"),
    FURNACES("piece","Interakcje"), ANVILS("kowadla","Interakcje"),
    ENCHANTING("zaklinanie","Interakcje"), BREWING("alchemia","Interakcje"),
    DOORS("drzwi","Interakcje"), BUTTONS("przyciski","Interakcje"),
    LEVERS("dzwignie","Interakcje"), PRESSURE_PLATES("plytki","Interakcje"),
    HOPPERS("leje","Interakcje"), ITEM_FRAMES("ramki","Interakcje"),
    ARMOR_STANDS("stojaki","Interakcje"),
    VEHICLES("pojazdy","Interakcje"), PORTALS("portale","Ruch"),
    ENDER_PEARLS("perly","Ruch"), ITEMS_DROP("wyrzucanie","Przedmioty"),
    ITEMS_PICKUP("podnoszenie","Przedmioty");

    private final String label;
    private final String category;
    RegionFlag(String label,String category){this.label=label;this.category=category;}
    public String label(){return label;}
    public String category(){return category;}
    public static RegionFlag parse(String input){
        String text=input.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(f->f.label.equals(text)||f.name().equalsIgnoreCase(text))
                .findFirst().orElseThrow(()->new IllegalArgumentException("Nieznana zasada: "+input));
    }
}
