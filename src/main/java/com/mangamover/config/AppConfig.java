package com.mangamover.config;

public class AppConfig {
    public static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8765"));
    public static final String DB_PATH = System.getenv().getOrDefault("DB_PATH", "manga_mover.db");
}
