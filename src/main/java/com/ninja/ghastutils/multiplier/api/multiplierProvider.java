
package com.ninja.ghastutils.multiplier.api;

import com.ninja.ghastutils.GhastUtils;

public final class multiplierProvider {
    private static volatile MultiplierManagerinter instance;

    public static MultiplierManagerinter getAPI() {
        return instance;
    }

    public static void initialize(GhastUtils plugin) {
        if (instance == null) {
            synchronized(multiplierProvider.class) {
                if (instance == null) {
                    instance = new gutilmultiplierimpl(plugin);
                }
            }
        }

    }

    private multiplierProvider() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
