package pl.lokos.tools.anticheat;

/** Wartości domyślne generowanego AntiCheat.json. */
public final class AntiCheatConfig {
    private boolean movementCheck=true;
    private boolean reachCheck=true;
    private boolean placeBurstCheck=true;
    private boolean redstoneProtection=true;
    private boolean explosionProtection=true;
    private boolean entitySpawnProtection=true;
    private double maximumHorizontalPerMove=1.8;
    private int movementViolations=8;
    private double maximumAttackDistance=7.5;
    private int blocksPerSecond=32;
    private int redstoneUpdatesPerChunkSecond=180;
    private int tntPerChunkFiveSeconds=18;
    private int creatureSpawnsPerChunkFiveSeconds=85;
    private int hopperTransfersPerChunkFiveSeconds=320;
    private int pistonCyclesPerChunkFiveSeconds=240;
    private int alertCooldownSeconds=8;
    private int joinGraceSeconds=6;
    private String alertFormat="&#FF727F✘ ANTYCHEAT &#A8A8B7» &#FFD166{player} &7• {check} &8({details})";
    public boolean movementCheck(){return movementCheck;}
    public boolean reachCheck(){return reachCheck;}
    public boolean placeBurstCheck(){return placeBurstCheck;}
    public boolean redstoneProtection(){return redstoneProtection;}
    public boolean explosionProtection(){return explosionProtection;}
    public boolean entitySpawnProtection(){return entitySpawnProtection;}
    public double maximumHorizontalPerMove(){return maximumHorizontalPerMove;}
    public int movementViolations(){return movementViolations;}
    public double maximumAttackDistance(){return maximumAttackDistance;}
    public int blocksPerSecond(){return blocksPerSecond;}
    public int redstoneUpdatesPerChunkSecond(){return redstoneUpdatesPerChunkSecond;}
    public int tntPerChunkFiveSeconds(){return tntPerChunkFiveSeconds;}
    public int creatureSpawnsPerChunkFiveSeconds(){return creatureSpawnsPerChunkFiveSeconds;}
    public int hopperTransfersPerChunkFiveSeconds(){return hopperTransfersPerChunkFiveSeconds;}
    public int pistonCyclesPerChunkFiveSeconds(){return pistonCyclesPerChunkFiveSeconds;}
    public int alertCooldownSeconds(){return alertCooldownSeconds;}
    public int joinGraceSeconds(){return joinGraceSeconds;}
    public String alertFormat(){return alertFormat;}
    public void validate(){
        if(maximumHorizontalPerMove<1.0||maximumHorizontalPerMove>10||
                movementViolations<3||movementViolations>50||
                maximumAttackDistance<5||maximumAttackDistance>20||
                blocksPerSecond<12||blocksPerSecond>200||
                redstoneUpdatesPerChunkSecond<50||redstoneUpdatesPerChunkSecond>10000||
                tntPerChunkFiveSeconds<4||tntPerChunkFiveSeconds>500||
                creatureSpawnsPerChunkFiveSeconds<20||creatureSpawnsPerChunkFiveSeconds>1000||
                hopperTransfersPerChunkFiveSeconds<100||hopperTransfersPerChunkFiveSeconds>10000||
                pistonCyclesPerChunkFiveSeconds<80||pistonCyclesPerChunkFiveSeconds>10000||
                alertCooldownSeconds<1||alertCooldownSeconds>300||
                joinGraceSeconds<1||joinGraceSeconds>60||
                alertFormat==null||alertFormat.length()>300||
                !alertFormat.contains("{player}")||!alertFormat.contains("{check}")||
                !alertFormat.contains("{details}"))
            throw new IllegalArgumentException("Niepoprawne wartości AntiCheat.json.");
    }
}
