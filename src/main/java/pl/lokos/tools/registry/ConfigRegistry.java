package pl.lokos.tools.registry;

import com.google.gson.*;
import pl.lokos.tools.config.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;

/**
 * Pliki definicji: MySql.json, Commands.json, Ranks.json, Regions.json.
 * Dawny config.json jest kopiowany do .legacy-backup dopiero po pomyslnej migracji.
 */
public final class ConfigRegistry {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path pluginDirectory;
    private final JsonConfigManager json;
    private ToolsConfig tools;
    private CommandsFile commands;
    private DefinitionFiles definitions;

    public ConfigRegistry(Path pluginDirectory) {
        this.pluginDirectory=pluginDirectory;
        this.json=new JsonConfigManager(pluginDirectory);
    }
    public void loadAll() throws IOException {
        Files.createDirectories(pluginDirectory);
        boolean firstRanks=Files.notExists(pluginDirectory.resolve("Ranks.json"));
        boolean firstRegions=Files.notExists(pluginDirectory.resolve("Regions.json"));
        Path old=pluginDirectory.resolve("config.json");
        JsonObject legacy=null;
        if(Files.exists(old)){
            try {
                legacy=JsonParser.parseString(Files.readString(old,StandardCharsets.UTF_8)).getAsJsonObject();
            } catch(RuntimeException e){
                throw new IOException("Nie można zmigrować config.json: "+e.getMessage(),e);
            }
        }
        if(legacy!=null) {
            JsonObject oldDatabase=legacy.getAsJsonObject("database");
            if(oldDatabase!=null){
                JsonObject mysql=oldDatabase.deepCopy();
                if(legacy.has("autosaveSeconds"))
                    mysql.add("autosaveSeconds",legacy.get("autosaveSeconds").deepCopy());
                seed("MySql.json",mysql,null);
            }
            JsonObject legacyCommands=legacy.getAsJsonObject("commands");
            JsonObject commandSettings=GSON.toJsonTree(new CommandsFile()).getAsJsonObject();
            if(legacyCommands!=null && legacyCommands.has("tools"))
                commandSettings.add("tools",legacyCommands.get("tools").deepCopy());
            seed("Commands.json",commandSettings,null);
            seed("Ranks.json",legacy.get("ranks"),"settings");
            seed("Regions.json",legacy.get("regions"),"settings");
        }
        ToolsConfig.Database mysql=json.load("MySql.json",ToolsConfig.Database.class,
                ToolsConfig.Database::new,ToolsConfig.Database::validate);
        commands=json.load("Commands.json",CommandsFile.class,CommandsFile::new,CommandsFile::validate);
        RanksFile ranks=json.load("Ranks.json",RanksFile.class,RanksFile::new,RanksFile::validate);
        RegionsFile regions=json.load("Regions.json",RegionsFile.class,RegionsFile::new,RegionsFile::validate,
                root -> {
                    if(root.has("settings") && root.get("settings").isJsonObject()){
                        JsonObject options=root.getAsJsonObject("settings");
                        if(options.has("barTitle") && options.get("barTitle").getAsString()
                                .equals("&aᴏʙꜱᴢᴀʀ &8» &7"))
                            options.addProperty("barTitle","&7ʟᴏᴋᴀʟɪᴢᴀᴄᴊᴀ &8» ");
                    }
                });
        tools=new ToolsConfig();
        tools.configure(mysql,ranks.settings(),regions.settings());
        definitions=new DefinitionFiles(pluginDirectory,ranks,regions,firstRanks,firstRegions);
        if(legacy!=null){
            Path backup=pluginDirectory.resolve("config.json.legacy-backup");
            if(Files.notExists(backup)) Files.copy(old,backup);
            Files.delete(old); // wymagana tylko przy poprawnym wczytaniu czterech nowych plikow
        }
    }

    private void seed(String file,JsonElement source,String wrapper) throws IOException {
        Path target=pluginDirectory.resolve(file);
        if(Files.exists(target) || source==null || source.isJsonNull()) return;
        JsonObject output;
        if(wrapper==null) output=source.getAsJsonObject().deepCopy();
        else {
            output=GSON.toJsonTree(wrapper.equals("settings")&&file.equals("Ranks.json")
                    ? new RanksFile() : new RegionsFile()).getAsJsonObject();
            output.add(wrapper,source.deepCopy());
        }
        // Wstępny plik tworzymy atomowo, po walidacji przez loader w kroku nastepnym.
        Path temporary=Files.createTempFile(pluginDirectory,".tools-migrate-", ".tmp");
        try{
            Files.writeString(temporary,GSON.toJson(output)+"\n",StandardCharsets.UTF_8);
            try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporary,target);}
        }finally{Files.deleteIfExists(temporary);}
    }

    public ToolsConfig tools(){return Objects.requireNonNull(tools);}
    public CommandsFile commands(){return Objects.requireNonNull(commands);}
    public DefinitionFiles definitions(){return Objects.requireNonNull(definitions);}
}
