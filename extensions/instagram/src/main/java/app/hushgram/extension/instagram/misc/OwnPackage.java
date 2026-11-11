/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.misc;

/**
 * Instagram's package name, used when checking that a running component belongs to this app.
 */
public final class OwnPackage {

    private OwnPackage() {}

    /** This build's package name. */
    public static String name() {
        return "com.instagram.android";
    }
}
