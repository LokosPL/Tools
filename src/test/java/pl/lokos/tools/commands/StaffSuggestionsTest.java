package pl.lokos.tools.commands;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

class StaffSuggestionsTest {
    private static Predicate<String> permissions(String... nodes) {
        return Set.of(nodes)::contains;
    }
    @Test void inventoryCompletesNicknameAfterEquipmentChoice(){
        assertEquals(List.of("Steve"),StaffSuggestions.forCommand(
                StaffCommand.Kind.INVENTORYOPEN,new String[]{"eq","St"},
                List.of("Steve","Alex"),permissions(),true));
        assertTrue(StaffSuggestions.forCommand(StaffCommand.Kind.INVENTORYOPEN,
                new String[]{"enderchest","St"},List.of("Steve"),permissions(),true).isEmpty());
        assertEquals(List.of("Steve"),StaffSuggestions.forCommand(
                StaffCommand.Kind.INVENTORYOPEN,new String[]{"enderchest","St"},
                List.of("Steve"),permissions("tools.inventoryopen.enderchest"),true));
    }
    @Test void permissionGatesNamesAndSecondArguments(){
        assertFalse(StaffSuggestions.forCommand(StaffCommand.Kind.TP,new String[]{""},
                List.of("Steve"),permissions(),true).contains("*"));
        assertTrue(StaffSuggestions.forCommand(StaffCommand.Kind.TP,new String[]{""},
                List.of("Steve"),permissions("tools.tp.all"),true).contains("*"));
        assertTrue(StaffSuggestions.forCommand(StaffCommand.Kind.VANISH,new String[]{"Steve",""},
                List.of("Steve"),permissions(),true).isEmpty());
        assertEquals(List.of("wlacz","wylacz"),StaffSuggestions.forCommand(
                StaffCommand.Kind.VANISH,new String[]{"Steve",""},
                List.of("Steve"),permissions("tools.vanish.others"),true));
        assertTrue(StaffSuggestions.forCommand(StaffCommand.Kind.GAMEMODE,
                new String[]{"Steve",""},List.of("Steve"),permissions(),true).isEmpty());
        assertEquals(List.of("1","2","3","4"),StaffSuggestions.forCommand(
                StaffCommand.Kind.GAMEMODE,new String[]{"Steve",""},
                List.of("Steve"),permissions("tools.gamemode.others"),true).stream()
                .filter(s->s.length()==1).toList());
    }
    @Test void deniedGroupOrOtherTeleportHasNoArgumentHints(){
        assertTrue(StaffSuggestions.forCommand(StaffCommand.Kind.TP,
                new String[]{"*",""},List.of("Steve"),permissions(),true).isEmpty());
        assertTrue(StaffSuggestions.forCommand(StaffCommand.Kind.TP,
                new String[]{"Steve",""},List.of("Steve"),permissions(),true).isEmpty());
        assertFalse(StaffSuggestions.forCommand(StaffCommand.Kind.TP,
                new String[]{"*",""},List.of("Steve"),
                permissions("tools.tp.all"),true).isEmpty());
    }

    @Test void onlyPassedVisiblePlayerNamesAreExposed(){
        var options=StaffSuggestions.forCommand(StaffCommand.Kind.TP,new String[]{""},
                List.of("Steve"),permissions(),true);
        assertTrue(options.contains("Steve"));
        assertFalse(options.contains("HiddenStaff"));
    }
    @Test void speedCompletesFourthParameterOnlyWithPermission(){
        var denied=StaffSuggestions.forCommand(StaffCommand.Kind.SPEED,
                new String[]{"fly","5","St"},List.of("Steve"),permissions(),true);
        assertTrue(denied.isEmpty());
        assertEquals(List.of("Steve"),StaffSuggestions.forCommand(StaffCommand.Kind.SPEED,
                new String[]{"fly","5","St"},List.of("Steve"),
                permissions("tools.speed.others"),true));
    }
}
