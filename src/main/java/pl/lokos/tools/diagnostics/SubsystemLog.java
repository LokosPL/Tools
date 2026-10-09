package pl.lokos.tools.diagnostics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Oddzielne kategorie logowania: SQL, RANGI, REGIONY, KONFIGURACJA i ADMINISTRACJA. */
public final class SubsystemLog {
    private SubsystemLog() {}
    public static Logger sql() { return LoggerFactory.getLogger("Tools.SQL"); }
    public static Logger ranks() { return LoggerFactory.getLogger("Tools.Rangi"); }
    public static Logger regions() { return LoggerFactory.getLogger("Tools.Regiony"); }
    public static Logger configuration() { return LoggerFactory.getLogger("Tools.Konfiguracja"); }
    public static Logger administration() { return LoggerFactory.getLogger("Tools.Administracja"); }
}
