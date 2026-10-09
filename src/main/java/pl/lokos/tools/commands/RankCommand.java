package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.database.RankRepository;
import pl.lokos.tools.helpers.Colors;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.manager.RankSnapshot;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class RankCommand implements BasicCommand {
    private static final String INFO = "&8[&#58C7FFTools&8] &7";
    private static final List<String> ACTIONS = List.of(
            "stworz", "dodaj", "pozycja", "wejscie", "usun", "nadaj", "edytuj", "info");

    private final JavaPlugin plugin;
    private final RankManager ranks;
    private final RankRepository repository;

    public RankCommand(JavaPlugin plugin, RankManager ranks) {
        this.plugin = plugin;
        this.ranks = ranks;
        this.repository = ranks.repository();
    }

    @Override
    public String permission() {
        return "tools.ranga.admin";
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            help(sender);
            return;
        }
        String operation = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (operation) {
                case "stworz" -> {
                    if (args.length != 4) { usage(sender, "/ranga stworz <nazwa> <prefix> <sufix>"); return; }
                    String name = rankName(args[1]);
                    String prefix = value(args[2]);
                    String suffix = value(args[3]);
                    perform(sender, () -> ranks.change(() -> repository.create(name, prefix, suffix)),
                            "Utworzono range &b" + name + "&7. Aby mozna bylo ja nadawac, ustaw /ranga pozycja.");
                }
                case "dodaj" -> {
                    if (args.length != 3) { usage(sender, "/ranga dodaj <ranga> <uprawnienie>"); return; }
                    String name = rankName(args[1]);
                    String permission = args[2].toLowerCase(Locale.ROOT);
                    if (!permission.equals("*") && !permission.matches("[a-z0-9_.:-]{1,128}")) {
                        throw new IllegalArgumentException("Niepoprawne uprawnienie.");
                    }
                    perform(sender, () -> ranks.change(() -> repository.addPermission(name, permission)),
                            "Dodano uprawnienie &b" + permission + "&7 do rangi &e" + name);
                }
                case "pozycja" -> {
                    if (args.length != 3) { usage(sender, "/ranga pozycja <ranga> <1-9998>"); return; }
                    String name = rankName(args[1]);
                    int position = Integer.parseInt(args[2]);
                    if (position < 1 || position > 9998) throw new IllegalArgumentException("Pozycja od 1 do 9998.");
                    perform(sender, () -> ranks.change(() -> repository.position(name, position)),
                            "Ranga &b" + name + "&7 ma pozycje &e" + position + "&7 i mozna ja nadawac.");
                }
                case "wejscie" -> {
                    if (args.length < 3) { usage(sender, "/ranga wejscie <ranga> <tekst|brak>"); return; }
                    String name = rankName(args[1]);
                    String text = join(args, 2);
                    if (text.equalsIgnoreCase("brak")) text = "";
                    if (text.length() > 512) throw new IllegalArgumentException("Tekst maksymalnie 512 znakow.");
                    final String message = text;
                    perform(sender, () -> ranks.change(() -> repository.joinMessage(name, message)),
                            message.isBlank() ? "Wylaczono komunikat wejscia." : "Zmieniono komunikat wejscia rangi &b" + name);
                }
                case "usun" -> {
                    if (args.length != 2) { usage(sender, "/ranga usun <ranga>"); return; }
                    String name = rankName(args[1]);
                    perform(sender, () -> ranks.change(() -> repository.delete(name)),
                            "Usunieto range &b" + name + "&7 oraz jej przypisania.");
                }
                case "edytuj" -> {
                    if (args.length < 4) { usage(sender, "/ranga edytuj <ranga> <prefix|sufix|nazwa> <wartosc>"); return; }
                    String name = rankName(args[1]);
                    String field = args[2].toLowerCase(Locale.ROOT);
                    String value = join(args, 3);
                    if (field.equals("nazwa")) value = rankName(value);
                    if (value.length() > 256) throw new IllegalArgumentException("Wartosc jest za dluga.");
                    final String newValue = value;
                    perform(sender, () -> ranks.change(() -> repository.edit(name, field, newValue)),
                            "Zmieniono &b" + field + "&7 rangi &e" + name);
                }
                case "nadaj" -> grant(sender, args);
                case "info" -> showInfo(sender, args);
                default -> help(sender);
            }
        } catch (IllegalArgumentException ex) {
            say(sender, "&c" + ex.getMessage());
        }
    }

    private void grant(CommandSender sender, String[] args) {
        if (args.length < 4 || args.length > 5) {
            usage(sender, "/ranga nadaj <nick> <ranga> <1d|12h|30m|na_zawsze>");
            return;
        }
        String target = args[1];
        String name = rankName(args[2]);
        RankSnapshot.Rank rank = ranks.snapshot().ranks().get(name);
        if (rank == null) throw new IllegalArgumentException("Nie ma takiej rangi.");
        if (!rank.assignable()) throw new IllegalArgumentException("Ustaw najpierw pozycje tej rangi.");
        String duration = join(args, 3);
        Long expires = parseTime(duration);
        Player online = Bukkit.getPlayerExact(target);
        CompletableFuture<UUID> lookup = online != null
                ? CompletableFuture.completedFuture(online.getUniqueId())
                : repository.findPlayer(target);

        perform(sender, () -> lookup.thenCompose(uuid -> {
            if (uuid == null) throw new IllegalArgumentException("Ten gracz musi najpierw wejsc na serwer.");
            return ranks.change(() -> repository.grant(uuid, name, expires));
        }), "Nadano range &b" + name + "&7 graczowi &e" + target + "&7: &a" + duration);
    }

    public static Long parseTime(String duration) {
        String text = duration.toLowerCase(Locale.ROOT).trim();
        if (text.equals("na_zawsze") || text.equals("na zawsze") || text.equals("zawsze")) return null;
        if (!text.matches("[1-9][0-9]{0,6}[mhdw]"))
            throw new IllegalArgumentException("Czas: 30m / 12h / 7d / 2w / na_zawsze.");
        long number = Long.parseLong(text.substring(0, text.length() - 1));
        long milliseconds = switch (text.charAt(text.length() - 1)) {
            case 'm' -> 60_000L;
            case 'h' -> 3_600_000L;
            case 'd' -> 86_400_000L;
            case 'w' -> 604_800_000L;
            default -> throw new IllegalArgumentException("Niepoprawny czas.");
        };
        return Math.addExact(System.currentTimeMillis(), Math.multiplyExact(number, milliseconds));
    }

    private void showInfo(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String names = ranks.snapshot().ranks().values().stream()
                    .sorted(Comparator.comparingInt(r -> r.position() == null ? 9999 : r.position()))
                    .map(RankSnapshot.Rank::name).collect(Collectors.joining("&7, &b"));
            say(sender, "Dostepne rangi: &b" + (names.isEmpty() ? "brak" : names));
            return;
        }
        if (args.length != 2) { usage(sender, "/ranga info [ranga]"); return; }
        String name = rankName(args[1]);
        RankSnapshot.Rank rank = ranks.snapshot().ranks().get(name);
        if (rank == null) throw new IllegalArgumentException("Nie ma takiej rangi.");
        repository.count(name).whenComplete((count, error) -> onMain(sender, () -> {
            if (error != null) { say(sender, "&cNie udalo sie odczytac danych z MySQL."); return; }
            say(sender, "Ranga: &b" + rank.name() + " &7| pozycja: &e"
                    + (rank.position() == null ? "nieustawiona" : rank.position())
                    + "&7 | graczy: &e" + count);
            say(sender, "Prefix: " + rank.prefix() + " &7| Sufix: " + rank.suffix());
            Set<String> permissions = ranks.snapshot().permissions().getOrDefault(name, Set.of());
            say(sender, "Uprawnienia: &b" + (permissions.isEmpty() ? "brak" : String.join("&7, &b", permissions)));
            say(sender, "Tekst wejscia: &b" + (rank.joinMessage() == null || rank.joinMessage().isBlank()
                    ? "wylaczony" : rank.joinMessage()));
        }));
    }

    private void perform(CommandSender sender, Supplier<CompletableFuture<Void>> action, String success) {
        try {
            action.get().whenComplete((unused, error) -> onMain(sender, () -> {
                if (error == null) say(sender, "&a" + success);
                else {
                    Throwable cause = error instanceof CompletionException && error.getCause() != null
                            ? error.getCause() : error;
                    say(sender, "&cBlad: " + (cause instanceof IllegalArgumentException
                            ? cause.getMessage() : "Nie mozna wykonac operacji. Sprawdz logi."));
                    plugin.getLogger().warning("Operacja rangi: " + cause.getMessage());
                }
            }));
        } catch (RuntimeException error) {
            say(sender, "&cBlad: " + error.getMessage());
        }
    }

    private void onMain(CommandSender sender, Runnable callback) {
        if (plugin.isEnabled()) plugin.getServer().getScheduler().runTask(plugin, callback);
    }

    private void say(CommandSender sender, String text) {
        sender.sendMessage(Colors.color(INFO + text));
    }

    private void usage(CommandSender sender, String syntax) {
        say(sender, "&cUzycie: &e" + syntax);
    }

    private void help(CommandSender sender) {
        say(sender, "&b/ranga stworz &7<nazwa> <prefix> <sufix>");
        say(sender, "&b/ranga dodaj &7<ranga> <uprawnienie, np. *>");
        say(sender, "&b/ranga pozycja &7<ranga> <numer>");
        say(sender, "&b/ranga wejscie &7<ranga> <tekst|brak>");
        say(sender, "&b/ranga nadaj &7<nick> <ranga> <czas|na_zawsze>");
        say(sender, "&b/ranga edytuj &7<ranga> <prefix|sufix|nazwa> <wartosc>");
        say(sender, "&b/ranga info &7[ranga] &8| &b/ranga usun &7<ranga>");
    }

    private static String join(String[] args, int first) {
        return String.join(" ", Arrays.copyOfRange(args, first, args.length));
    }

    private static String value(String raw) {
        return raw.replace('_', ' ');
    }

    private static String rankName(String raw) {
        String name = raw.toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9_-]{1,24}"))
            throw new IllegalArgumentException("Nazwa rangi: litery a-z, cyfry, _ i -, do 24 znakow.");
        return name;
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 0) return ACTIONS;
        if (args.length == 1) return filter(ACTIONS, args[0]);
        String action = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (action.equals("nadaj")) return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
            if (Set.of("dodaj", "pozycja", "wejscie", "usun", "edytuj", "info").contains(action))
                return filter(ranks.snapshot().ranks().keySet(), args[1]);
        }
        if (args.length == 3) {
            if (action.equals("dodaj")) {
                List<String> perms = new ArrayList<>(List.of("*"));
                perms.addAll(Bukkit.getPluginManager().getPermissions().stream().map(Permission::getName).toList());
                return filter(perms, args[2]);
            }
            if (action.equals("nadaj")) return filter(ranks.snapshot().ranks().values().stream()
                    .filter(RankSnapshot.Rank::assignable).map(RankSnapshot.Rank::name).toList(), args[2]);
            if (action.equals("edytuj")) return filter(List.of("prefix", "sufix", "nazwa"), args[2]);
            if (action.equals("wejscie")) return filter(List.of("brak", "&a{nick}_wszedl_na_serwer!"), args[2]);
        }
        if (action.equals("nadaj") && args.length >= 4) return filter(List.of("1h", "1d", "7d", "30d", "na_zawsze"), args[3]);
        if (action.equals("pozycja") && args.length == 3) return filter(List.of("1", "2", "3", "4", "5", "10"), args[2]);
        return List.of();
    }

    private static List<String> filter(Collection<String> values, String entered) {
        String prefix = entered.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER).limit(80).toList();
    }
}
