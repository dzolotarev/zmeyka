package com.dzolotarev.zmeyka;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Рекорд лежит в ~/.snake/highscore.properties — без внешних БД, всё локально. */
public class HighScoreStore {

    private static final String KEY = "best.score";

    private final Path file;

    public HighScoreStore(Path file) {
        this.file = file;
    }

    public static HighScoreStore defaultStore() {
        Path dir = Path.of(System.getProperty("user.home"), ".snake");
        return new HighScoreStore(dir.resolve("highscore.properties"));
    }

    public long read() {
        Properties props = load();
        String raw = props.getProperty(KEY);
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public void write(long score) {
        Properties props = load();
        props.setProperty(KEY, Long.toString(score));
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (var out = Files.newOutputStream(file)) {
                props.store(out, "snake high score");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить рекорд в " + file, e);
        }
    }

    private Properties load() {
        Properties props = new Properties();
        if (Files.exists(file)) {
            try (var in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException("Не удалось прочитать рекорд из " + file, e);
            }
        }
        return props;
    }
}
