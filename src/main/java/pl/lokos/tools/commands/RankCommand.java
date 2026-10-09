package pl.lokos.tools.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lokos.tools.database.RankRepository;
import pl.lokos.tools.helpers.Messages;
import pl.lokos.tools.helpers.RankValidity;
import java.time.ZoneId;
import pl.lokos.tools.manager.RankManager;
import pl.lokos.tools.manager.RankSnapshot;
import pl.lokos.tools.inventorys.RankMenuFactory;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

public final class RankCommand implements BasicCommand {
    private static final List<String> ACTIONS = List.of(
            "stworz", "dodaj", "pozycja", "wejscie", "usun",
            "nadaj", "edytuj", "info", "lista", "sprawdz", "menu", "pomoc");

    private final JavaPlugin plugin;
    private final RankManager ranks;
    private final RankRepository repository;
    private final String commandPermission;
    private final RankMenuFactory menus;

    public RankCommand(JavaPlugin plugin, RankManager ranks, String permission,RankMenuFactory menus) {
        this.plugin = plugin;
        this.ranks = ranks;
        this.repository = ranks.repository();
        this.commandPermission=permission;
        this.menus=menus;
    }

    @Override
    public String permission() {
        return commandPermission;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            if(sender instanceof Player p)menus.list(p,0);
            else help(sender);
            return;
        }
        String operation = args[0].toLowerCase(Locale.ROOT);
        if(operation.equals("pomoc")){help(sender);return;}
        if(operation.equals("menu")){if(sender instanceof Player p)menus.list(p,0);else help(sender);return;}
        try {
            switch (operation) {
                case "stworz" -> {
                    if (args.length != 3 && args.length != 4) {
                        errorUsage(sender, "Podaj nazwę rangi i prefix. Sufix jest opcjonalny.",
                                "/ranga stworz <nazwa> <prefix> [sufix]");
                        return;
                    }
                    String name = rankName(args[1]);
                    String prefix = displayValue(args[2]);
                    String suffix = args.length == 4 ? displayValue(args[3]) : "";
                    if (ranks.snapshot().ranks().containsKey(name)) {
                        Messages.error(sender, "Ranga &c&n" + name + "&r&c już istnieje.");
                        Messages.hint(sender, "Jeśli chcesz ją zmienić, użyj &a/ranga edytuj " + name + " prefix <tekst>");
                        return;
                    }
                    checkLength("Prefix", prefix, 256);
                    checkLength("Suffix", suffix, 256);
                    perform(sender, () -> ranks.create(name, prefix, suffix),
                            "Utworzono rangę &a" + name + "&a. &7Następny krok: &a/ranga pozycja " + name + " 1");
                }
                case "dodaj" -> {
                    if (args.length != 3) {
                        errorUsage(sender, "Podaj istniejącą rangę oraz uprawnienie.",
                                "/ranga dodaj <ranga> <uprawnienie>");
                        return;
                    }
                    String name = rankName(args[1]);
                    if (!requireRank(sender, name)) return;
                    String permission = args[2].toLowerCase(Locale.ROOT);
                    if (!permission.equals("*") && !permission.matches("[a-z0-9_.:-]{1,128}")) {
                        errorUsage(sender, "Niepoprawne uprawnienie: &c&n" + args[2] + "&r&c.",
                                "/ranga dodaj <ranga> <np. essentials.fly lub *>");
                        return;
                    }
                    perform(sender, () -> ranks.addPermission(name, permission),
                            "Dodano uprawnienie &a" + permission + "&a do rangi &a" + name + "&a.");
                }
                case "pozycja" -> {
                    if (args.length != 3) {
                        errorUsage(sender, "Pozycja musi być liczbą od 1 do 9998.",
                                "/ranga pozycja <ranga> <1-9998>");
                        return;
                    }
                    String name = rankName(args[1]);
                    if (!requireRank(sender, name)) return;
                    int position;
                    try {
                        position = Integer.parseInt(args[2]);
                    } catch (NumberFormatException exception) {
                        errorUsage(sender, "Nieprawidłowa pozycja: &c&n" + args[2] + "&r&c. Wpisz liczbę.",
                                "/ranga pozycja " + name + " <1-9998>");
                        return;
                    }
                    if (position < 1 || position > 9998) {
                        errorUsage(sender, "Pozycja musi wynosić od 1 do 9998.",
                                "/ranga pozycja " + name + " <1-9998>");
                        return;
                    }
                    perform(sender, () -> ranks.position(name, position),
                            "Ustawiono pozycję &a" + position + "&a rangi &a" + name + "&a. Można już ją nadawać.");
                }
                case "wejscie" -> {
                    if (args.length < 3) {
                        errorUsage(sender, "Podaj tekst powitania albo słowo brak.",
                                "/ranga wejscie <ranga> <tekst|brak>");
                        return;
                    }
                    String name = rankName(args[1]);
                    if (!requireRank(sender, name)) return;
                    String value = join(args, 2).replace('_', ' ');
                    if (value.equalsIgnoreCase("brak")) value = "";
                    checkLength("Komunikat wejścia", value, 512);
                    final String message = value;
                    perform(sender, () -> ranks.joinMessage(name, message),
                            message.isBlank() ? "Wyłączono wiadomość wejścia rangi &a" + name + "&a."
                                    : "Ustawiono wiadomość wejścia rangi &a" + name + "&a.");
                }
                case "usun" -> {
                    if (args.length != 2) {
                        errorUsage(sender, "Wskaż nazwę rangi do usunięcia.", "/ranga usun <ranga>");
                        return;
                    }
                    String name = rankName(args[1]);
                    if (!requireRank(sender, name)) return;
                    perform(sender, () -> ranks.delete(name),
                            "Usunięto rangę &a" + name + "&a i jej przypisania.");
                }
                case "edytuj" -> {
                    if(args.length==2 && sender instanceof Player player){
                        String name=rankName(args[1]);
                        if(requireRank(sender,name))menus.details(player,name);
                        return;
                    }
                    if (args.length < 4) {
                        errorUsage(sender, "Wybierz pole do zmiany: prefix, sufix lub nazwa.",
                                "/ranga edytuj <ranga> <prefix|sufix|nazwa> <wartość>");
                        return;
                    }
                    String name = rankName(args[1]);
                    if (!requireRank(sender, name)) return;
                    String field = args[2].toLowerCase(Locale.ROOT);
                    if (!Set.of("prefix", "sufix", "nazwa").contains(field)) {
                        errorUsage(sender, "Nieznane pole: &c&n" + args[2] + "&r&c.",
                                "/ranga edytuj " + name + " <prefix|sufix|nazwa> <wartość>");
                        return;
                    }
                    String value = join(args, 3);
                    if (field.equals("nazwa")) value = rankName(value);
                    else value = value.replace('_', ' ');
                    checkLength("Wartość", value, 256);
                    final String newValue = value;
                    perform(sender, () -> ranks.edit(name, field, newValue),
                            "Zmieniono &a" + field + "&a rangi &a" + name + "&a.");
                }
                case "nadaj" -> grant(sender, args);
                case "sprawdz" -> checkGrant(sender, args);
                case "lista" -> list(sender, args);
                case "info" -> showInfo(sender, args);
                default -> {
                    Messages.error(sender, "Nieznana podkomenda: &c&n" + args[0] + "&r&c.");
                    Messages.hint(sender, "Użyj &a/ranga &7aby zobaczyć dostępne komendy.");
                }
            }
        } catch (IllegalArgumentException ex) {
            Messages.error(sender, ex.getMessage());
        }
    }

    private void grant(CommandSender sender, String[] args) {
        if (args.length < 4 || args.length > 5) {
            errorUsage(sender, "Podaj nick, rangę i czas ważności.",
                    "/ranga nadaj <nick> <ranga> <1d|12h|30m|*>");
            return;
        }
        String target = args[1];
        String name = rankName(args[2]);
        RankSnapshot.Rank rank = ranks.snapshot().ranks().get(name);
        if (rank == null) {
            Messages.error(sender, "Nie znaleziono rangi &c&n" + name + "&r&c.");
            Messages.hint(sender, "Wpisz &a/ranga lista&7, aby zobaczyć istniejące rangi.");
            return;
        }
        if (!rank.assignable()) {
            Messages.error(sender, "Ranga &c&n" + name + "&r&c nie ma ustawionej pozycji.");
            Messages.hint(sender, "Najpierw wpisz &a/ranga pozycja " + name + " 1");
            return;
        }
        String duration = join(args, 3);
        Long expires = parseTime(duration);
        Player online = Bukkit.getPlayerExact(target);
        CompletableFuture<UUID> lookup = online != null
                ? CompletableFuture.completedFuture(online.getUniqueId())
                : repository.findPlayer(target);
        perform(sender, () -> lookup.thenCompose(uuid -> {
            if (uuid == null)
                throw new IllegalArgumentException("Gracz &c&n" + target
                        + "&r&c nie został znaleziony w bazie. Musi najpierw wejść na serwer.");
            return ranks.change(() -> repository.grant(uuid, name, expires));
        }), "Nadano rangę &a" + name + "&7 graczowi &a" + target + "&7. Ważność: &a" + RankValidity.durationLabel(duration) + "&7.");
    }

    private void checkGrant(CommandSender sender, String[] args) {
        if (args.length != 2) {
            errorUsage(sender, "Podaj nick gracza.", "/ranga sprawdz <nick>");
            return;
        }
        String nick = args[1];
        Player online = Bukkit.getPlayerExact(nick);
        CompletableFuture<UUID> lookup = online != null
                ? CompletableFuture.completedFuture(online.getUniqueId())
                : repository.findPlayer(nick);

        lookup.thenCompose(uuid -> {
            if (uuid == null) {
                return CompletableFuture.<RankSnapshot.Grant>failedFuture(
                        new IllegalArgumentException("Nie znaleziono gracza " + nick + "."));
            }
            return repository.findGrant(uuid);
        }).whenComplete((grant, error) -> onMain(() -> {
            if (error != null) {
                Messages.error(sender, "Nie można sprawdzić rangi. " + readableError(error));
                return;
            }
            Messages.title(sender, "RANGA GRACZA " + nick);
            long now = System.currentTimeMillis();
            if (grant == null) {
                Messages.info(sender, "Przypisanie: &7brak rangi");
                Messages.info(sender, "Ważność: &7nie dotyczy");
            } else {
                Messages.info(sender, "Ranga w bazie: &a" + grant.rank());
                Messages.info(sender, "Ważność: " + (grant.active(now) ? "&a" : "&c")
                        + RankValidity.remaining(grant.expiresAt(), now));
                if (grant.expiresAt() != null) {
                    Messages.info(sender, "Data wygaśnięcia: &7"
                            + RankValidity.expirationDate(grant.expiresAt(), ZoneId.systemDefault()));
                }
            }
            Player current = Bukkit.getPlayerExact(nick);
            if (current != null) {
                RankSnapshot.Rank inCache = ranks.snapshot().forPlayer(current.getUniqueId());
                Messages.info(sender, "Widoczna na serwerze: &a" + (inCache == null ? "Gracz" : inCache.name()));
                Messages.info(sender, "Gracz jest online: &aTak");
            } else {
                Messages.info(sender, "Gracz jest offline; uprawnienia zastosują się po wejściu.");
            }
        }));
    }

    private static String readableError(Throwable original) {
        Throwable cause = original;
        while ((cause instanceof CompletionException
                || cause instanceof java.util.concurrent.ExecutionException)
                && cause.getCause() != null) cause = cause.getCause();
        if (cause instanceof IllegalArgumentException) return cause.getMessage();
        if (cause instanceof java.sql.SQLException sql) {
            return "Błąd MySQL (kod " + sql.getErrorCode() + ", stan " + sql.getSQLState() + ").";
        }
        return "Zobacz konsolę serwera.";
    }

    public static Long parseTime(String duration) {
        String value = duration.toLowerCase(Locale.ROOT).trim();
        if (value.equals("*") || value.equals("na_zawsze") || value.equals("na zawsze") || value.equals("zawsze")) return null;
        if (!value.matches("[1-9][0-9]{0,6}[mhdw]")) {
            throw new IllegalArgumentException("Nieprawidłowy czas: &c&n" + duration
                    + "&r&c. Dozwolone: &a30m&c, &a12h&c, &a7d&c, &a2w&c, &a*&c lub &ana_zawsze&c.");
        }
        long count = Long.parseLong(value.substring(0, value.length() - 1));
        long millis = switch (value.charAt(value.length() - 1)) {
            case 'm' -> 60_000L;
            case 'h' -> 3_600_000L;
            case 'd' -> 86_400_000L;
            case 'w' -> 604_800_000L;
            default -> throw new IllegalArgumentException("Nieznana jednostka czasu.");
        };
        return Math.addExact(System.currentTimeMillis(), Math.multiplyExact(count, millis));
    }

    private void list(CommandSender sender, String[] args) {
        if (args.length != 1) {
            errorUsage(sender, "Ta komenda nie przyjmuje dodatkowych argumentów.", "/ranga lista");
            return;
        }
        List<RankSnapshot.Rank> all = sortedRanks();
        Messages.line(sender," ");
        Messages.title(sender, "LISTA RANG");
        Messages.line(sender,"&7  Nazwa &8• &7Priorytet &8• &7Stan");
        Messages.line(sender," ");
        if (all.isEmpty()) {
            Messages.info(sender, "Nie utworzono jeszcze żadnej rangi.");
            Messages.hint(sender, "Pierwszą rangę dodasz przez &a/ranga stworz");
            return;
        }
        int index = 1;
        for (RankSnapshot.Rank rank : all) {
            String position = rank.position() == null ? "&8nieustawiona" : "&a" + rank.position();
            Messages.line(sender, "&8" + index++ + ". &7" + rank.name()
                    + " &8│ &7Pozycja: " + position
                    + (rank.assignable() ? " &8│ &aGotowa" : " &8│ &cNiegotowa"));
        }
        Messages.line(sender," ");
        Messages.hint(sender, "Szczegóły: &a/ranga info <nazwa>");
    }

    private List<RankSnapshot.Rank> sortedRanks() {
        return ranks.snapshot().ranks().values().stream()
                .sorted(Comparator
                        .comparingInt((RankSnapshot.Rank r) -> r.position() == null ? 9999 : r.position())
                        .thenComparing(RankSnapshot.Rank::name))
                .toList();
    }

    private void showInfo(CommandSender sender, String[] args) {
        if (args.length == 1) {
            list(sender, new String[]{"lista"});
            return;
        }
        if (args.length != 2) {
            errorUsage(sender, "Podaj jedną nazwę rangi.", "/ranga info <ranga>");
            return;
        }
        String name = rankName(args[1]);
        RankSnapshot.Rank rank = ranks.snapshot().ranks().get(name);
        if (rank == null) {
            Messages.error(sender, "Ranga &c&n" + name + "&r&c nie istnieje.");
            Messages.hint(sender, "Sprawdź listę przez &a/ranga lista");
            return;
        }
        repository.count(name).whenComplete((count, error) -> onMain(() -> {
            if (error != null) {
                Messages.error(sender, "Nie udało się odczytać liczby graczy z bazy.");
                return;
            }
            Messages.title(sender, "RANGA " + rank.name().toUpperCase(Locale.ROOT));
            Messages.info(sender, "Pozycja: &a" + (rank.position() == null ? "nieustawiona" : rank.position()));
            Messages.info(sender, "Graczy z rangą: &a" + count);
            Messages.info(sender, "Prefix: " + rank.prefix() + " &8│ &7Sufix: " + rank.suffix());
            Set<String> granted = ranks.snapshot().permissions().getOrDefault(name, Set.of());
            List<String> available = menus.permissions().stream().filter(granted::contains).toList();
            Messages.info(sender, "Funkcje Tools: &a"
                    + (available.isEmpty() ? "brak" : String.join("&7, &a",available)));
            Messages.hint(sender, "Zarządzaj dostępem: &a/ranga edytuj " + name);
            Messages.info(sender, "Powitanie: &a"
                    + (rank.joinMessage() == null || rank.joinMessage().isBlank()
                    ? "wyłączone" : rank.joinMessage()));
        }));
    }

    private void perform(CommandSender sender, Supplier<CompletableFuture<Void>> action, String success) {
        try {
            action.get().whenComplete((unused, error) -> onMain(() -> {
                if (error == null) {
                    Messages.success(sender, success);
                } else {
                    Throwable cause = error instanceof CompletionException && error.getCause() != null
                            ? error.getCause() : error;
                    if (cause instanceof IllegalArgumentException) {
                        Messages.error(sender, cause.getMessage());
                    } else {
                        Messages.error(sender, cause instanceof IllegalStateException
                                ? cause.getMessage()
                                : "Nie wykonano operacji. " + readableError(cause));
                        plugin.getLogger().warning("Błąd operacji rang: " + cause.getMessage());
                    }
                }
            }));
        } catch (RuntimeException error) {
            Messages.error(sender, error.getMessage());
        }
    }

    private void onMain(Runnable callback) {
        if (plugin.isEnabled()) plugin.getServer().getScheduler().runTask(plugin, callback);
    }

    private boolean requireRank(CommandSender sender, String name) {
        if (ranks.snapshot().ranks().containsKey(name)) return true;
        Messages.error(sender, "Nie znaleziono rangi &c&n" + name + "&r&c.");
        Messages.hint(sender, "Dostępne rangi sprawdzisz przez &a/ranga lista");
        return false;
    }

    private void errorUsage(CommandSender sender, String error, String usage) {
        Messages.error(sender, error);
        Messages.usage(sender, usage);
    }

    private void help(CommandSender sender) {
        Messages.line(sender," ");
        Messages.title(sender, "ZARZĄDZANIE RANGAMI");
        Messages.line(sender,"&7  Tworzenie i konfiguracja");
        Messages.line(sender, "&a/ranga stworz &7<nazwa> <prefix> [sufix]");
        Messages.line(sender, "&a/ranga dodaj &7<ranga> <uprawnienie>");
        Messages.line(sender, "&a/ranga pozycja &7<ranga> <1-9998>");
        Messages.line(sender, "&a/ranga wejscie &7<ranga> <tekst|brak>");
        Messages.line(sender," ");
        Messages.line(sender,"&7  Nadawanie i administracja");
        Messages.line(sender, "&a/ranga nadaj &7<nick> <ranga> <czas|*|na_zawsze>");
        Messages.line(sender, "&a/ranga edytuj &7<ranga> <prefix|sufix|nazwa> <wartość>");
        Messages.line(sender, "&a/ranga info &7<ranga> &8• &a/ranga lista");
        Messages.line(sender, "&a/ranga usun &7<ranga>");
        Messages.line(sender, "&a/ranga sprawdz &7<nick> &8- &7odczyt przypisania z MySQL");
        Messages.line(sender," ");
    }

    public static String rankName(String raw) {
        String name = Normalizer.normalize(raw.strip(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        if (!name.matches("[\\p{IsLatin}0-9_-]{1,24}")) {
            throw new IllegalArgumentException("Niepoprawna nazwa rangi: &c&n" + raw
                    + "&r&c. Użyj do 24 znaków: liter (także ą, ć, ł, ó), cyfr, _ lub -.");
        }
        return name;
    }

    private static void checkLength(String name, String text, int limit) {
        if (text.length() > limit) {
            throw new IllegalArgumentException(name + " jest za długi. Maksymalnie &a" + limit + "&c znaków.");
        }
    }

    private static String displayValue(String raw) {
        return raw.replace('_', ' ');
    }

    private static String join(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 0) return ACTIONS;
        if (args.length == 1) return filter(ACTIONS, args[0]);
        String action = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (action.equals("nadaj") || action.equals("sprawdz"))
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
            if (Set.of("dodaj", "pozycja", "wejscie", "usun", "edytuj", "info").contains(action))
                return filter(ranks.snapshot().ranks().keySet(), args[1]);
        }
        if (args.length == 3) {
            if (action.equals("dodaj")) {
                List<String> permissions = new ArrayList<>(List.of("*"));
                permissions.addAll(Bukkit.getPluginManager().getPermissions().stream()
                        .map(Permission::getName).toList());
                return filter(permissions, args[2]);
            }
            if (action.equals("nadaj")) return filter(sortedRanks().stream()
                    .filter(RankSnapshot.Rank::assignable).map(RankSnapshot.Rank::name).toList(), args[2]);
            if (action.equals("edytuj")) return filter(List.of("prefix", "sufix", "nazwa"), args[2]);
            if (action.equals("wejscie")) return filter(List.of("brak", "&a{nick}_dołączył_na_serwer!"), args[2]);
            if (action.equals("pozycja")) return filter(List.of("1", "2", "3", "4", "5", "10"), args[2]);
        }
        if (action.equals("nadaj") && args.length >= 4)
            return filter(List.of("*", "1h", "1d", "7d", "30d", "na_zawsze"), args[3]);
        return List.of();
    }

    private static List<String> filter(Collection<String> values, String entered) {
        String prefix = entered.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER).limit(80).toList();
    }
}
