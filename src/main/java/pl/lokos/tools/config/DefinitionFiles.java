package pl.lokos.tools.config;

import com.google.gson.*;
import pl.lokos.tools.region.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Atomowe aktualizacje JSON; wyłącznie definicje, nigdy przypisania graczy. */
public final class DefinitionFiles {
    private final Path folder;
    private final Gson gson=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private volatile RanksFile ranks;
    private volatile RegionsFile regions;
    private final boolean importRanks;
    private final boolean importRegions;

    public DefinitionFiles(Path folder,RanksFile ranks,RegionsFile regions,boolean importRanks,boolean importRegions){
        this.folder=folder;this.ranks=ranks;this.regions=regions;
        this.importRanks=importRanks;this.importRegions=importRegions;
    }
    public boolean importRanks(){return importRanks;}
    public boolean importRegions(){return importRegions;}
    public RanksFile ranks(){return ranks;}
    public RegionsFile regions(){return regions;}
    public synchronized void saveRanks(RanksFile next) {
        next.validate();
        atomic("Ranks.json",next);
        ranks=next;
    }
    public synchronized void saveRegions(RegionsFile next) {
        next.validate();
        atomic("Regions.json",next);
        regions=next;
    }
    private void atomic(String name,Object next){
        Path file=folder.resolve(name);
        try{
            Path temp=Files.createTempFile(folder,".tools-", ".tmp");
            try{
                Files.writeString(temp,gson.toJson(next)+"\n",StandardCharsets.UTF_8);
                try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
            }finally{Files.deleteIfExists(temp);}
        }catch(IOException e){throw new IllegalStateException("Nie zapisano "+name+"; sprawdź uprawnienia do plików.",e);}
    }
}
