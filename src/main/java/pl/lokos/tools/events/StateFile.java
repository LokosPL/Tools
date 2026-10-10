package pl.lokos.tools.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import pl.lokos.tools.config.JsonConfigManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Jeden sekwencyjny, asynchroniczny, atomowy zapis dla stanu modułu. */
public final class StateFile<T> implements AutoCloseable {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final ExecutorService executor;
    private final Path path;
    private volatile T state;

    public StateFile(Path folder,String filename,Class<T> type,Supplier<T> defaults,Consumer<T> validator) throws IOException{
        if(!filename.matches("[a-zA-Z0-9_-]+\\.json"))throw new IllegalArgumentException("Nazwa pliku.");
        path=folder.resolve(filename);
        state=new JsonConfigManager(folder).load(filename,type,defaults,validator);
        executor=Executors.newSingleThreadExecutor(r->{
            Thread t=new Thread(r,"Tools-Stan-"+filename);t.setDaemon(false);return t;
        });
    }
    public T get(){return state;}
    public synchronized CompletableFuture<Void> update(UnaryOperator<T> change){
        T old=state,next=change.apply(old);
        if(next.equals(old))return CompletableFuture.completedFuture(null);
        state=next;
        return write(next).whenComplete((v,error)->{
            if(error!=null)synchronized(this){if(state==next)state=old;}
        });
    }
    public synchronized CompletableFuture<Void> flush(){return write(state);}
    private CompletableFuture<Void> write(T snapshot){
        return CompletableFuture.runAsync(()->{
            Path tmp=null;
            try{
                tmp=Files.createTempFile(path.getParent(),".tools-state-",".tmp");
                Files.writeString(tmp,GSON.toJson(snapshot)+"\n",StandardCharsets.UTF_8);
                try{Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException ex){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}
            }catch(IOException error){throw new CompletionException(error);}
            finally{if(tmp!=null)try{Files.deleteIfExists(tmp);}catch(IOException ignored){}}
        },executor);
    }
    @Override public void close(){
        executor.shutdown();
        try{if(!executor.awaitTermination(5,TimeUnit.SECONDS))executor.shutdownNow();}
        catch(InterruptedException ex){Thread.currentThread().interrupt();executor.shutdownNow();}
    }
}
