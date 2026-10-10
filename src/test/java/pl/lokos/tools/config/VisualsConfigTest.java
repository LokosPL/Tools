package pl.lokos.tools.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class VisualsConfigTest {
    @TempDir Path directory;
    @Test void loadsPaletteWithPreservedUserColors() throws Exception {
        var manager=new JsonConfigManager(directory);
        var defaults=manager.load("Visuals.json",VisualsConfig.class,
                VisualsConfig::new,VisualsConfig::validate);
        assertEquals("#FFD166",defaults.gold());
        assertEquals("#70D6E8",defaults.cyan());
        Path saved=directory.resolve("Visuals.json");
        Files.writeString(saved,Files.readString(saved).replace("#70D6E8","#64BDEB"));
        assertEquals("#64BDEB",manager.load("Visuals.json",VisualsConfig.class,
                VisualsConfig::new,VisualsConfig::validate).cyan());
    }
}
