
package com.ninja.ghastutils.multiplier;

public class PlayerMultiplier {
    private double boosterMultiplier = (double)1.0F;
    private long boosterExpiration = 0L;
    private double petMultiplier = (double)0.0F;
    private boolean petActive = false;

    public double getBoosterMultiplier() {
        return this.boosterMultiplier;
    }

    public void setBoosterMultiplier(double boosterMultiplier) {
        this.boosterMultiplier = boosterMultiplier;
    }

    public long getBoosterExpiration() {
        return this.boosterExpiration;
    }

    public void setBoosterExpiration(long boosterExpiration) {
        this.boosterExpiration = boosterExpiration;
    }

    public double getPetMultiplier() {
        return this.petMultiplier;
    }

    public void setPetMultiplier(double petMultiplier) {
        this.petMultiplier = petMultiplier;
    }

    public boolean isPetActive() {
        return this.petActive;
    }

    public void setPetActive(boolean petActive) {
        this.petActive = petActive;
    }
}
