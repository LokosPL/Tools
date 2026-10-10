package pl.lokos.tools.config;

import java.util.*;

/** Domyślna paleta HEX generowana do Visuals.json; istniejące HEX mają pierwszeństwo. */
public final class VisualsConfig {
    private String gold="#FFD166";
    private String cyan="#70D6E8";
    private String gray="#A8A8B7";
    private String muted="#737388";
    private String red="#FF727F";
    private String success="#89E5B0";
    private String surface="#242334";
    public String gold(){return gold;}
    public String cyan(){return cyan;}
    public String gray(){return gray;}
    public String muted(){return muted;}
    public String red(){return red;}
    public String success(){return success;}
    public String surface(){return surface;}
    public void validate(){
        for(var color:List.of(gold,cyan,gray,muted,red,success,surface))
            if(color==null || !color.matches("#[0-9a-fA-F]{6}"))
                throw new IllegalArgumentException("Visuals.json: kolory muszą mieć postać #RRGGBB.");
    }
}
