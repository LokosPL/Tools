package pl.lokos.tools.helpers;

import org.bukkit.command.CommandSender;
import pl.lokos.tools.config.CommandTextRegistry;

/**
 * Wszystkie wiadomości własnej komendy przechodzą przez jej JSON, bez
 * powielania kodu lub sprawdzania kontekstu wątku (działa też po async).
 */
public final class CommandMessages {
    private final String command;
    public CommandMessages(String command) { this.command=command; }
    private String translate(String raw) { return CommandTextRegistry.rewrite(command,raw); }
    public void info(CommandSender sender,String text) { Messages.info(sender,translate(text)); }
    public void success(CommandSender sender,String text) { Messages.success(sender,translate(text)); }
    public void error(CommandSender sender,String text) { Messages.error(sender,translate(text)); }
    public void line(CommandSender sender,String text) { Messages.line(sender,translate(text)); }
    public void title(CommandSender sender,String text) { Messages.title(sender,translate(text)); }
    public void usage(CommandSender sender,String text) { Messages.usage(sender,translate(text)); }
    public void hint(CommandSender sender,String text) { Messages.hint(sender,translate(text)); }
    public void unknown(CommandSender sender) { Messages.unknown(sender); }
}
