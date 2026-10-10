package pl.lokos.tools.config;

/** Wartości domyślne w Javie; przy starcie powstaje Security.json. */
public final class SecurityConfig {
    private boolean premiumSkins = true;
    private boolean antiBot = true;

    public boolean premiumSkins(){return premiumSkins;}
    public boolean antiBot(){return antiBot;}
    public void validate(){}
}
