package pl.lokos.tools.config;

import org.bukkit.command.CommandSender;
import pl.lokos.tools.helpers.Messages;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Immutable snapshot tekstów w osobnym JSON każdej komendy. */
public final class CommandTextRegistry {
    private static volatile CommandTextRegistry active;
    private final Path directory;
    private final AtomicReference<Map<String,CommandTextFile>> snapshot =
            new AtomicReference<>(Map.of());

    public CommandTextRegistry(Path pluginDirectory) {
        this.directory=pluginDirectory.resolve("commands");
    }
    public Map<String,CommandTextFile> loadSnapshot() throws IOException {
        JsonConfigManager loader=new JsonConfigManager(directory);
        Map<String,CommandTextFile> next=new LinkedHashMap<>();
        for(String name:CommandTextFile.names()) {
            CommandTextFile config=loader.load(name+".json",CommandTextFile.class,
                    ()->CommandTextFile.defaults(name),CommandTextFile::validate, root -> {
                        // Starszy plik z /region bypass aktualizujemy automatycznie.
                        if(!name.equals("region") || !root.has("help")
                                || !root.get("help").isJsonArray())return;
                        com.google.gson.JsonArray clean=new com.google.gson.JsonArray();
                        for(var item:root.getAsJsonArray("help")) {
                            if(!item.isJsonPrimitive()) { clean.add(item); continue; }
                            String line=item.getAsString();
                            if(line.contains("/region bypass")) {
                                if(!line.contains("/region stan"))continue;
                                int split=line.indexOf(" &8•");
                                line=split<0?line.substring(0,line.indexOf("/region bypass"))
                                        :line.substring(0,split);
                            }
                            clean.add(line);
                        }
                        root.add("help",clean);
                    });
            next.put(name,config);
        }
        return Map.copyOf(next);
    }
    public void install(Map<String,CommandTextFile> next) {
        Objects.requireNonNull(next);
        if(!next.keySet().containsAll(CommandTextFile.names()))
            throw new IllegalArgumentException("Brak tekstów jednej z komend.");
        snapshot.set(Map.copyOf(next));
        active=this;
    }
    public static String text(String command,String key) {
        CommandTextRegistry registry=active;
        CommandTextFile file=registry==null?null:registry.snapshot.get().get(command);
        String value=file==null?null:file.message(key);
        if(value!=null)return value;
        return CommandTextFile.defaults(command).message(key);
    }
    public static String text(String command,String key,Map<String,String> vars) {
        String template=text(command,key);
        if(template==null)return "";
        for(var e:vars.entrySet())
            template=template.replace("{"+e.getKey()+"}",
                    e.getValue()==null?"":e.getValue().replace("&",""));
        return template;
    }
    /**
     * Zamienia każdą statyczną część wiadomości — także w dynamicznych
     * komunikatach z nazwą gracza, rangi lub regionu. Nie zmienia zmiennych.
     * Poprawki z plików commands/*.json działają także w callbackach async.
     */
    public static String rewrite(String command,String original) {
        CommandTextRegistry registry=active;
        CommandTextFile file=registry==null?null:registry.snapshot.get().get(command);
        if(file==null || original==null)return original;
        String output=original;
        for(var e:file.replacements().entrySet()) {
            if(!e.getKey().equals(e.getValue()))output=output.replace(e.getKey(),e.getValue());
        }
        return output;
    }
    public static void help(CommandSender sender,String command) {
        CommandTextRegistry registry=active;
        CommandTextFile file=registry==null?null:registry.snapshot.get().get(command);
        if(file==null)file=CommandTextFile.defaults(command);
        Messages.title(sender,file.title());
        for(String line:file.help())Messages.line(sender,line);
    }
    public static void error(CommandSender sender,String command,String key) {
        Messages.error(sender,text(command,key));
    }
    public static void info(CommandSender sender,String command,String key) {
        Messages.info(sender,text(command,key));
    }
    public static void success(CommandSender sender,String command,String key) {
        Messages.success(sender,text(command,key));
    }
}
