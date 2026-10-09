package pl.lokos.tools.database;

import pl.lokos.tools.region.Region;
import pl.lokos.tools.region.RegionFlag;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Zapisy SQL tylko poprzez kolejke DatabaseManager; nigdy na watku tickow. */
public final class RegionRepository {
    private final DatabaseExecutor database;
    private final SqlDialect dialect;
    public RegionRepository(DatabaseExecutor database) { this.database = database; this.dialect = database.dialect(); }

    public record Data(List<Region> regions, String mainSpawn) { }

    public CompletableFuture<Data> load() {
        return database.query(c -> {
            Map<String, Map<RegionFlag, Boolean>> flags = new HashMap<>();
            try (PreparedStatement q = c.prepareStatement("SELECT region_name, flag_name, allowed FROM tools_region_flags");
                 ResultSet rs = q.executeQuery()) {
                while (rs.next()) {
                    flags.computeIfAbsent(rs.getString(1), unused -> new EnumMap<>(RegionFlag.class))
                            .put(RegionFlag.valueOf(rs.getString(2)), rs.getBoolean(3));
                }
            }
            List<Region> all = new ArrayList<>();
            try (PreparedStatement q = c.prepareStatement("SELECT name,world_uuid,min_x,max_x,min_z,max_z,parent,entry_rank,spawn_x,spawn_y,spawn_z,spawn_yaw,spawn_pitch FROM tools_regions");
                 ResultSet rs = q.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    double x = rs.getDouble("spawn_x");
                    Region.Spawn spawn = rs.wasNull() ? null : new Region.Spawn(x,rs.getDouble("spawn_y"),
                            rs.getDouble("spawn_z"),rs.getFloat("spawn_yaw"),rs.getFloat("spawn_pitch"));
                    all.add(new Region(name, UUID.fromString(rs.getString("world_uuid")),
                            rs.getInt("min_x"),rs.getInt("max_x"),rs.getInt("min_z"),rs.getInt("max_z"),
                            rs.getString("parent"),rs.getString("entry_rank"),
                            flags.getOrDefault(name, Map.of()),spawn));
                }
            }
            String main = null;
            try (PreparedStatement q = c.prepareStatement("SELECT config_value FROM tools_region_settings WHERE config_key='spawn'");
                 ResultSet rs = q.executeQuery()) { if (rs.next()) main = rs.getString(1); }
            return new Data(List.copyOf(all),main);
        });
    }

    /** Tworzenie regionu i jego wstepnych flag w jednej transakcji. */
    public CompletableFuture<Void> create(Region r) {
        return database.query(c -> {
            boolean original=c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                try(PreparedStatement p=c.prepareStatement(
                        "INSERT INTO tools_regions(name,world_uuid,min_x,max_x,min_z,max_z,parent) VALUES(?,?,?,?,?,?,?)")) {
                    p.setString(1,r.name()); p.setString(2,r.world().toString());
                    p.setInt(3,r.minX());p.setInt(4,r.maxX());
                    p.setInt(5,r.minZ());p.setInt(6,r.maxZ());
                    p.setString(7,r.parent());p.executeUpdate();
                }
                if(!r.flags().isEmpty()) {
                    try(PreparedStatement p=c.prepareStatement(
                            "INSERT INTO tools_region_flags(region_name,flag_name,allowed) VALUES(?,?,?)")) {
                        for(var entry:r.flags().entrySet()) {
                            p.setString(1,r.name());p.setString(2,entry.getKey().name());
                            p.setBoolean(3,entry.getValue());p.addBatch();
                        }
                        p.executeBatch();
                    }
                }
                c.commit();
            } catch(Exception error) {
                c.rollback();throw error;
            } finally {
                c.setAutoCommit(original);
            }
            return null;
        });
    }

    public CompletableFuture<Void> remove(String name) {
        return database.query(c -> {
            try (PreparedStatement p=c.prepareStatement("DELETE FROM tools_regions WHERE name=?")) {
                p.setString(1,name);
                if(p.executeUpdate()==0) throw new SQLException("Nie znaleziono regionu.");
            }
            return null;
        });
    }

    public CompletableFuture<Void> flag(String name, RegionFlag flag, Boolean state) {
        return database.query(c -> {
            if (state==null) {
                try(PreparedStatement p=c.prepareStatement(
                        "DELETE FROM tools_region_flags WHERE region_name=? AND flag_name=?")) {
                    p.setString(1,name);p.setString(2,flag.name());p.executeUpdate();
                }
            } else {
                try(PreparedStatement p=c.prepareStatement(dialect.regionFlag())) {
                    p.setString(1,name);p.setString(2,flag.name());p.setBoolean(3,state);p.executeUpdate();
                }
            }
            return null;
        });
    }

    public CompletableFuture<Void> entryRank(String name, String rank) {
        return database.query(c -> {
            try(PreparedStatement p=c.prepareStatement("UPDATE tools_regions SET entry_rank=? WHERE name=?")) {
                p.setString(1,rank);p.setString(2,name);
                if(p.executeUpdate()==0) throw new SQLException("Nie znaleziono regionu lub brak zmian.");
            }
            return null;
        });
    }

    public CompletableFuture<Void> setSpawn(String name, Region.Spawn location) {
        return database.query(c -> {
            // Zachowujemy transakcyjnosc: wspolny spawn i cel regionu musza
            // zostac zapisane razem.
            boolean original = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                try(PreparedStatement p=c.prepareStatement(
                        "UPDATE tools_regions SET spawn_x=?,spawn_y=?,spawn_z=?,spawn_yaw=?,spawn_pitch=? WHERE name=?")) {
                    p.setDouble(1,location.x());p.setDouble(2,location.y());p.setDouble(3,location.z());
                    p.setFloat(4,location.yaw());p.setFloat(5,location.pitch());p.setString(6,name);
                    if(p.executeUpdate()==0) throw new SQLException("Nie znaleziono regionu.");
                }
                try(PreparedStatement p=c.prepareStatement(dialect.regionSetting())) {
                    p.setString(1,name);p.executeUpdate();
                }
                c.commit();
            } catch(Exception e) {
                c.rollback(); throw e;
            } finally {
                c.setAutoCommit(original);
            }
            return null;
        });
    }
}
