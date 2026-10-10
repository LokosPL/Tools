package pl.lokos.tools.events;

import java.util.List;

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
    private boolean challengesEnabled=true;
    private int challengeFlushSeconds=5;
    private List<Integer> challengeKeyRewards=List.of(1,2,3);
    private boolean meteorEnabled=true;
    private int meteorSpawnIntervalSeconds=180;
    private int meteorLifetimeSeconds=480;
    private int meteorMaxActive=3;
    private int meteorRadiusBlocks=48;
    private int meteorMinimumDistanceBlocks=12;
    private int meteorKeysPerMeteor=1;
    private double meteorRareChance=0.05;
    private int meteorRareKeys=3;
    private boolean meteorAnnouncements=true;
    private boolean meteorEffects=true;
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
    public boolean challengesEnabled(){return challengesEnabled;}
    public int challengeFlushSeconds(){return challengeFlushSeconds;}
    public List<Integer> challengeKeyRewards(){return List.copyOf(challengeKeyRewards);}
    public boolean meteorEnabled(){return meteorEnabled;}
    public int meteorSpawnIntervalSeconds(){return meteorSpawnIntervalSeconds;}
    public int meteorLifetimeSeconds(){return meteorLifetimeSeconds;}
    public int meteorMaxActive(){return meteorMaxActive;}
    public int meteorRadiusBlocks(){return meteorRadiusBlocks;}
    public int meteorMinimumDistanceBlocks(){return meteorMinimumDistanceBlocks;}
    public int meteorKeysPerMeteor(){return meteorKeysPerMeteor;}
    public double meteorRareChance(){return meteorRareChance;}
    public int meteorRareKeys(){return meteorRareKeys;}
    public boolean meteorAnnouncements(){return meteorAnnouncements;}
    public boolean meteorEffects(){return meteorEffects;}
    public int snowParticleCount(){return snowParticleCount;}
    public String eventBar(){return eventBar;}
    public String tokenName(){return tokenName;}
    public String tokenLore(){return tokenLore;}
    public void validate(){
        if(defaultDurationMinutes<1||maxDurationMinutes<defaultDurationMinutes||
                maxDurationMinutes>43200||tokenChance<0||tokenChance>0.5||
                eventKeyChance<0||eventKeyChance>0.25||tokensForKey<1||tokensForKey>1000||
                minimumPvPKillIntervalSeconds<60||minimumPvPKillIntervalSeconds>3600||
                challengeFlushSeconds<1||challengeFlushSeconds>60||
                challengeKeyRewards==null||challengeKeyRewards.size()!=3||
                challengeKeyRewards.stream().anyMatch(n->n==null||n<1||n>16)||
                meteorSpawnIntervalSeconds<30||meteorSpawnIntervalSeconds>3600||
                meteorLifetimeSeconds<60||meteorLifetimeSeconds>3600||
                meteorMaxActive<1||meteorMaxActive>12||
                meteorRadiusBlocks<16||meteorRadiusBlocks>128||
                meteorMinimumDistanceBlocks<4||
                meteorMinimumDistanceBlocks>=meteorRadiusBlocks||
                meteorKeysPerMeteor<1||meteorKeysPerMeteor>16||
                meteorRareChance<0||meteorRareChance>0.5||
                meteorRareKeys<1||meteorRareKeys>16||
                snowParticleCount<0||snowParticleCount>50||
                eventBar==null||!eventBar.contains("{event}")||
                !eventBar.contains("{time}")||!eventBar.contains("{command}")||
                tokenName==null||tokenLore==null||
                tokenName.length()>200||tokenLore.length()>250||eventBar.length()>300)
            throw new IllegalArgumentException("Niepoprawne Events.json.");
    }
}
