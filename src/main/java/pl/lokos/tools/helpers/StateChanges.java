package pl.lokos.tools.helpers;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Brak zmiany nie jest sukcesem ani błędem infrastruktury.
 * Usługi rozpoznają go przed zapisem, a komendy i GUI pokazują neutralny komunikat.
 */
public final class StateChanges {
    private StateChanges() {}

    public static final class Unchanged extends RuntimeException {
        public Unchanged(String message){super(message);}
    }

    public static void requireChange(boolean already,String message) {
        if(already)throw new Unchanged(message);
    }
    public static Throwable root(Throwable error){
        Throwable current=error;
        while((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause()!=null)current=current.getCause();
        return current;
    }
    /** Zwraca true, jeżeli wysłano neutralny komunikat. */
    public static boolean reportUnchanged(org.bukkit.command.CommandSender sender, Throwable error){
        Throwable cause=root(error);
        if(cause instanceof Unchanged){
            Messages.unchanged(sender,cause.getMessage());
            return true;
        }
        return false;
    }
}
