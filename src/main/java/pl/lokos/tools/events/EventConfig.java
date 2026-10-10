package pl.lokos.tools.events;

/** Java definiuje domyślne wartości Events.json; w JSON pozostaje edytowalność. */
public final class EventConfig {
    private boolean enabled=true;
    private int defaultDurationMinutes=60;
    private int maxDurationMinutes=20160;
    private double tokenChance=0.02;
    private double eventKeyChance=0.003;
    private int tokensForKey=10;
    private int minimumPvPKillIntervalSeconds=300;
    private boolean snowParticles=true;
    private int snowParticleCount=12;
    private String eventBar="&#FFD166✦ EVENT: &#70D6E8{event} &#A8A8B7│ {time} &#FFD166│ /{command}";
    private String tokenName="&#FFD166✦ Pamiątka: &#70D6E8{event}";
    private String tokenLore="&#A8A8B7Zdobądź {needed} pamiątek, aby otrzymać klucz eventowy.";
    public boolean enabled(){return enabled;}
    public int defaultDurationMinutes(){return defaultDurationMinutes;}
    public int maxDurationMinutes(){return maxDurationMinutes;}
    public double tokenChance(){return tokenChance;}
    public double eventKeyChance(){return eventKeyChance;}
    public int tokensForKey(){return tokensForKey;}
    public int minimumPvPKillIntervalSeconds(){return minimumPvPKillIntervalSeconds;}
    public boolean snowParticles(){return snowParticles;}
    public int snowParticleCount(){return snowParticleCount;}
    public String eventBar(){return eventBar;}
    public String tokenName(){return tokenName;}
    public String tokenLore(){return tokenLore;}
    public void validate(){
        if(defaultDurationMinutes<1||maxDurationMinutes<defaultDurationMinutes||
                maxDurationMinutes>43200||tokenChance<0||tokenChance>0.5||
                eventKeyChance<0||eventKeyChance>0.25||tokensForKey<1||tokensForKey>1000||
                minimumPvPKillIntervalSeconds<60||minimumPvPKillIntervalSeconds>3600||
                snowParticleCount<0||snowParticleCount>50||
                eventBar==null||!eventBar.contains("{event}")||
                !eventBar.contains("{time}")||!eventBar.contains("{command}")||
                tokenName==null||tokenLore==null||
                tokenName.length()>200||tokenLore.length()>250||eventBar.length()>300)
            throw new IllegalArgumentException("Niepoprawne Events.json.");
    }
}
