package pl.lokos.tools.border;

/** Bezpieczna granica startowa 1500x1500 wokół głównego spawnu. */
public final class BorderConfig {
    private boolean enabled=true;
    private int initialDiameter=1500;
    private int maxDiameter=15000;
    private int expansionDiameter=1000;
    private int fallbackExpansionDiameter=500;
    private int cycleDays=7;
    private int fallbackDays=14;
    private int requiredActiveMinutes=7200;
    private int dailyPlayerCapMinutes=240;
    private int afkAfterMinutes=5;
    private boolean showProgressBossbar=true;
    private String bar="&#FFD166✦ NOWE TERENY &#A8A8B7│ {days} dni │ &#70D6E8{progress}%";
    public boolean enabled(){return enabled;}
    public int initialDiameter(){return initialDiameter;}
    public int maxDiameter(){return maxDiameter;}
    public int expansionDiameter(){return expansionDiameter;}
    public int fallbackExpansionDiameter(){return fallbackExpansionDiameter;}
    public int cycleDays(){return cycleDays;}
    public int fallbackDays(){return fallbackDays;}
    public int requiredActiveMinutes(){return requiredActiveMinutes;}
    public int dailyPlayerCapMinutes(){return dailyPlayerCapMinutes;}
    public int afkAfterMinutes(){return afkAfterMinutes;}
    public boolean showProgressBossbar(){return showProgressBossbar;}
    public String bar(){return bar;}
    public void validate(){
        if(initialDiameter<100||maxDiameter<initialDiameter||maxDiameter>30000000||
                expansionDiameter<2||fallbackExpansionDiameter<2||
                cycleDays<1||cycleDays>60||fallbackDays<cycleDays||fallbackDays>120||
                requiredActiveMinutes<1||requiredActiveMinutes>1000000||
                dailyPlayerCapMinutes<1||dailyPlayerCapMinutes>1440||
                afkAfterMinutes<1||afkAfterMinutes>60||
                bar==null||bar.length()>300||!bar.contains("{progress}"))
            throw new IllegalArgumentException("Błędne WorldBorder.json.");
    }
}
