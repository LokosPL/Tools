package pl.lokos.tools.crates;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CratesConfigActivityTest {
    @Test void legacyConfigWithoutActivityWindowKeepsItsSettings(){
        CratesConfig config=new Gson().fromJson(
                "{\"afkKeyMinutes\":90,\"ordinaryKeyChanceFromHostileMob\":0.04}",
                CratesConfig.class);
        config.validate();
        assertEquals(90,config.afkKeyMinutes());
        assertEquals(5,config.afkActivityWindowMinutes());
        assertEquals(0.04,config.ordinaryKeyChanceFromHostileMob());
    }
    @Test void badActivityWindowIsRejected(){
        CratesConfig config=new Gson().fromJson(
                "{\"afkActivityWindowMinutes\":0}",CratesConfig.class);
        assertThrows(IllegalArgumentException.class,config::validate);
    }
}
