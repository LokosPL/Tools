package pl.lokos.tools.msg;

/** Domyślne ustawienia generowanego PrivateMessages.json. */
public final class PrivateMessageConfig {
    private boolean enabled = true;
    private int cooldownMillis = 1500;
    private int maxLength = 240;
    private boolean protectAdminRanks = true;
    private String senderFormat = "&#A8A8B7[&#70D6E8Ja &#A8A8B7→ &#FFD166{target}&#A8A8B7] &f{message}";
    private String receiverFormat = "&#A8A8B7[&#FFD166{sender} &#A8A8B7→ &#70D6E8Ja&#A8A8B7] &f{message}";

    public boolean enabled(){return enabled;}
    public int cooldownMillis(){return cooldownMillis;}
    public int maxLength(){return maxLength;}
    public boolean protectAdminRanks(){return protectAdminRanks;}
    public String senderFormat(){return senderFormat;}
    public String receiverFormat(){return receiverFormat;}
    public void validate(){
        if(cooldownMillis<0 || cooldownMillis>60000)
            throw new IllegalArgumentException("PrivateMessages.json: cooldownMillis musi być w zakresie 0-60000.");
        if(maxLength<1 || maxLength>1024)
            throw new IllegalArgumentException("PrivateMessages.json: maxLength musi być w zakresie 1-1024.");
        if(senderFormat==null || receiverFormat==null || senderFormat.length()>500 || receiverFormat.length()>500
                || !senderFormat.contains("{message}") || !receiverFormat.contains("{message}"))
            throw new IllegalArgumentException("PrivateMessages.json: szablony muszą zawierać {message}.");
    }
}
