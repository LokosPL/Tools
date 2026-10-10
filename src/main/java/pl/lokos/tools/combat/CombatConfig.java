package pl.lokos.tools.combat;

/** Konfiguracja z Combat.json. Zwykła gra, lot elytrą i samouszkodzenie bez taga. */
public final class CombatConfig {
    private boolean enabled=true;
    private int tagSeconds=18;
    private boolean tagFromPlayers=true;
    private boolean tagFromMobs=true;
    private boolean logoutKillsPlayer=true;
    private boolean respawnAtMainSpawn=true;
    private boolean disableAdvancementAnnouncements=true;
    private String bossbar="&#FF727F⚔ WALKA &#A8A8B7│ &#FFD166Nie wychodź! &#A8A8B7{seconds}s";
    private String logout="&#FF727F✙ &#A8A8B7Gracz &#FFD166{player} &#A8A8B7wylogował się podczas walki!";
    private String death="&#FF727F✙ &#A8A8B7Gracz: &#FFD166{player} &#A8A8B7{reason}";
    public boolean enabled(){return enabled;}
    public int tagSeconds(){return tagSeconds;}
    public boolean tagFromPlayers(){return tagFromPlayers;}
    public boolean tagFromMobs(){return tagFromMobs;}
    public boolean logoutKillsPlayer(){return logoutKillsPlayer;}
    public boolean respawnAtMainSpawn(){return respawnAtMainSpawn;}
    public boolean disableAdvancementAnnouncements(){return disableAdvancementAnnouncements;}
    public String bossbar(){return bossbar;}
    public String logout(){return logout;}
    public String death(){return death;}
    public void validate(){
        if(tagSeconds<5||tagSeconds>120||bossbar==null||logout==null||death==null||
                !bossbar.contains("{seconds}")||!logout.contains("{player}")||
                !death.contains("{player}")||!death.contains("{reason}")||
                bossbar.length()>300||logout.length()>300||death.length()>300)
            throw new IllegalArgumentException("Błędne wartości Combat.json.");
    }
}
