package com.dzolotarev.zmeyka;

/** Версия выпуска — одна точка для окна, скриптов сборки и релиза. */
public final class Version {

    public static final String NUMBER = "1.0";
    public static final String ARTIFACT = "zmeyka";

    private Version() {
    }

    public static String full() {
        return ARTIFACT + " " + NUMBER;
    }
}
