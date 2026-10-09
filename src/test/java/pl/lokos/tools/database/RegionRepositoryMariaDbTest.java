package pl.lokos.tools.database;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import pl.lokos.tools.region.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/** Weryfikacja prawdziwych transakcji, foreign keys i flag w MariaDB. */
class RegionRepositoryMariaDbTest {
    @Test void createsLoadsNestedFlagsSpawnAndCascadingDeletion() throws Exception {
        String url=System.getenv("TOOLS_IT_DB_URL");
        Assumptions.assumeTrue(url!=null&&!url.isBlank(),"Wymagana MariaDB w CI.");
        String user=System.getenv().getOrDefault("TOOLS_IT_DB_USER","root");
        String pass=System.getenv().getOrDefault("TOOLS_IT_DB_PASS","");
        try(Connection c=DriverManager.getConnection(url,user,pass)) {
            DatabaseManager.createSchema(c);
        }
        DatabaseExecutor executor=new DatabaseExecutor() {
            @Override public <T> CompletableFuture<T> query(DatabaseManager.SqlOperation<T> operation) {
                try(Connection c=DriverManager.getConnection(url,user,pass)) {
                    return CompletableFuture.completedFuture(operation.run(c));
                } catch(Exception e) {return CompletableFuture.failedFuture(e);}
            }
        };
        RegionRepository repo=new RegionRepository(executor);
        UUID world=UUID.randomUUID();
        String rootName="s"+UUID.randomUUID().toString().substring(0,9);
        String childName="a"+UUID.randomUUID().toString().substring(0,9);
        Region root=new Region(rootName,world,-100,100,-100,100,null,null,Map.of(),null);
        Region child=new Region(childName,world,-10,10,-10,10,rootName,null,
                Map.of(RegionFlag.PVP,true,RegionFlag.BUILD,false),null);
        try {
            repo.create(root).join();
            repo.create(child).join();
            RegionRepository.Data data=repo.load().join();
            RegionIndex index=new RegionIndex(data.regions());
            assertEquals(childName,index.at(world,0,0).name());
            assertFalse(index.enabled(index.byName(childName),RegionFlag.BUILD));
            assertTrue(index.enabled(index.byName(childName),RegionFlag.PVP));
            repo.entryRank(childName,"moderator").join();
            repo.flag(childName,RegionFlag.PVP,null).join();
            repo.setSpawn(rootName,new Region.Spawn(.5,81.5,.5,90,5)).join();
            data=repo.load().join();
            assertEquals(rootName,data.mainSpawn());
            index=new RegionIndex(data.regions());
            assertEquals("moderator",index.byName(childName).entryRank());
            assertFalse(index.enabled(index.byName(childName),RegionFlag.PVP));
            assertNotNull(index.byName(rootName).spawn());
            repo.remove(rootName).join();
            assertNull(new RegionIndex(repo.load().join().regions()).byName(childName));
        } finally {
            try{repo.remove(rootName).join();}catch(Exception ignored){}
        }
    }
}
