package pl.lokos.tools.region;

import java.util.Arrays;
import java.util.Locale;

public enum RegionFlag {
    BUILD("budowanie"), BREAK("niszczenie"), PVP("pvp"), DAMAGE("obrazenia"),
    MOBS("moby"), EXPLOSIONS("wybuchy"), FLUIDS("plyny"), INTERACT("interakcje"),
    PISTONS("tloki"), FIRE("ogien");

    private final String label;
    RegionFlag(String label) { this.label = label; }
    public String label() { return label; }
    public static RegionFlag parse(String input) {
        String text = input.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(flag -> flag.label.equals(text) || flag.name().equalsIgnoreCase(text))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Nieznana flaga: " + input));
    }
}
