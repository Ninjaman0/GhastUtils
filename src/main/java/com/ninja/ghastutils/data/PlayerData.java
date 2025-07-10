
package com.ninja.ghastutils.data;

import com.ninja.ghastutils.multiplier.PlayerMultiplier;
import java.util.UUID;

public class PlayerData {
    private final UUID playerUuid;
    private final PlayerMultiplier multiplier;

    public PlayerData(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.multiplier = new PlayerMultiplier();
    }

    public UUID getPlayerUuid() {
        return this.playerUuid;
    }

    public PlayerMultiplier getMultiplier() {
        return this.multiplier;
    }
}
