package ru.annelo.player2statistic;

import java.util.UUID;

class PlayerStats {
    private UUID uuid;
    private String playerName;
    private int playTime; // В минутах
    private int mobsKilled;
    private int itemsEaten;
    private double distanceTraveled; // В блоках
    private int blocksBroken;
    private int deaths;
    private int itemsCrafted;
    private int itemsUsed;
    private int chestsOpened;
    private int messagesSent;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public int getPlayTime() {
        return playTime;
    }

    public void setPlayTime(int playTime) {
        this.playTime = playTime;
    }

    public int getMobsKilled() {
        return mobsKilled;
    }

    public void setMobsKilled(int mobsKilled) {
        this.mobsKilled = mobsKilled;
    }

    public int getItemsEaten() {
        return itemsEaten;
    }

    public void setItemsEaten(int itemsEaten) {
        this.itemsEaten = itemsEaten;
    }

    public double getDistanceTraveled() {
        return distanceTraveled;
    }

    public void setDistanceTraveled(double distanceTraveled) {
        this.distanceTraveled = distanceTraveled;
    }

    public int getBlocksBroken() {
        return blocksBroken;
    }

    public void setBlocksBroken(int blocksBroken) {
        this.blocksBroken = blocksBroken;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getItemsCrafted() {
        return itemsCrafted;
    }

    public void setItemsCrafted(int itemsCrafted) {
        this.itemsCrafted = itemsCrafted;
    }

    public int getItemsUsed() {
        return itemsUsed;
    }

    public void setItemsUsed(int itemsUsed) {
        this.itemsUsed = itemsUsed;
    }

    public int getChestsOpened() {
        return chestsOpened;
    }

    public void setChestsOpened(int chestsOpened) {
        this.chestsOpened = chestsOpened;
    }

    public int getMessagesSent() {
        return messagesSent;
    }

    public void setMessagesSent(int messagesSent) {
        this.messagesSent = messagesSent;
    }

    public void setPlayerName(String name) {
        this.playerName = name;
    }

    public String getPlayerName() {
        return playerName;
    }
}
