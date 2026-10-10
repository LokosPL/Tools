package pl.lokos.tools.staff;

/** Domyślne wartości generowanego StaffTools.json. */
public final class StaffConfig {
    private int helpopCooldownSeconds=30;
    private int helpopMaxLength=240;
    private int broadcastDefaultSeconds=30;
    private int broadcastMaxSeconds=2592000;
    private int maxTeleportCoordinate=29999984;
    private String vanishTag="&#FF727F✦ VANISH";
    private String vanishEnabled="&#70D6E8✔ &7Vanish został włączony.";
    private String vanishDisabled="&#70D6E8✔ &7Vanish został wyłączony.";
    private String staffAlert="&#FFD166✦ &7{player} &#A8A8B7» &7{action}";
    private String helpopFormat="&#FFD166✦ HELPOP &#A8A8B7» &#70D6E8{player}: &f{message}";
    private String broadcastTitle="&#FFD166✦ &#A8A8B7{message}";

    public int helpopCooldownSeconds(){return helpopCooldownSeconds;}
    public int helpopMaxLength(){return helpopMaxLength;}
    public int broadcastDefaultSeconds(){return broadcastDefaultSeconds;}
    public int broadcastMaxSeconds(){return broadcastMaxSeconds;}
    public int maxTeleportCoordinate(){return maxTeleportCoordinate;}
    public String vanishTag(){return vanishTag;}
    public String vanishEnabled(){return vanishEnabled;}
    public String vanishDisabled(){return vanishDisabled;}
    public String staffAlert(){return staffAlert;}
    public String helpopFormat(){return helpopFormat;}
    public String broadcastTitle(){return broadcastTitle;}
    public void validate(){
        if(helpopCooldownSeconds<0||helpopCooldownSeconds>600||helpopMaxLength<1||helpopMaxLength>1024
                ||broadcastDefaultSeconds<1||broadcastDefaultSeconds>broadcastMaxSeconds
                ||broadcastMaxSeconds>31536000||maxTeleportCoordinate<100||maxTeleportCoordinate>29999984)
            throw new IllegalArgumentException("Niepoprawne limity StaffTools.json.");
        for(String t:new String[]{vanishTag,vanishEnabled,vanishDisabled,staffAlert,helpopFormat,broadcastTitle})
            if(t==null||t.length()>500)throw new IllegalArgumentException("Niepoprawny tekst StaffTools.json.");
        if(!staffAlert.contains("{player}")||!staffAlert.contains("{action}")||
                !helpopFormat.contains("{player}")||!helpopFormat.contains("{message}")||
                !broadcastTitle.contains("{message}"))
            throw new IllegalArgumentException("Brak wymaganych zmiennych StaffTools.json.");
    }
}
