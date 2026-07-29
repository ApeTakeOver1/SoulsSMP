package net.ape.soulssmp.data;

import net.ape.soulssmp.api.SoulType;

public class PlayerData {

    private SoulType soul;

    private int mana;
    private int maxMana;

    private int favor;

    private int resurrectionCount;

    private boolean awakened;

    public PlayerData() {
        this.soul = null;
        this.mana = 0;
        this.maxMana = 0;
        this.favor = 0;
        this.resurrectionCount = 0;
        this.awakened = false;
    }

    // Soul

    public SoulType getSoul() {
        return soul;
    }

    public void setSoul(SoulType soul) {
        this.soul = soul;
    }

    // Mana

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = maxMana;
    }

    // Favor

    public int getFavor() {
        return favor;
    }

    public void setFavor(int favor) {
        this.favor = favor;
    }

    // Resurrection

    public int getResurrectionCount() {
        return resurrectionCount;
    }

    public void setResurrectionCount(int resurrectionCount) {
        this.resurrectionCount = resurrectionCount;
    }

    // Awakening

    public boolean isAwakened() {
        return awakened;
    }

    public void setAwakened(boolean awakened) {
        this.awakened = awakened;
    }

}