package ru.annelo.player2statistic;

import java.util.UUID;

import com.google.gson.JsonObject;

final class StatsJsonCodec {
    private StatsJsonCodec() {}

    static JsonObject toJson(PlayerStats stats) {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", stats.getUuid().toString());
        json.addProperty("player_name", stats.getPlayerName());
        json.addProperty("playTime", stats.getPlayTime());
        json.addProperty("mobsKilled", stats.getMobsKilled());
        json.addProperty("itemsEaten", stats.getItemsEaten());
        json.addProperty("distanceTraveled", stats.getDistanceTraveled());
        json.addProperty("blocksBroken", stats.getBlocksBroken());
        json.addProperty("deaths", stats.getDeaths());
        json.addProperty("itemsCrafted", stats.getItemsCrafted());
        json.addProperty("itemsUsed", stats.getItemsUsed());
        json.addProperty("chestsOpened", stats.getChestsOpened());
        json.addProperty("messagesSent", stats.getMessagesSent());
        return json;
    }

    static PlayerStats fromJson(JsonObject json, UUID fallbackUuid) {
        PlayerStats stats = new PlayerStats();
        String uuidString =
                json.has("uuid") ? json.get("uuid").getAsString() : fallbackUuid.toString();
        stats.setUuid(UUID.fromString(uuidString));
        stats.setPlayerName(json.has("player_name") ? json.get("player_name").getAsString() : null);
        stats.setPlayTime(json.has("playTime") ? json.get("playTime").getAsInt() : 0);
        stats.setMobsKilled(json.has("mobsKilled") ? json.get("mobsKilled").getAsInt() : 0);
        stats.setItemsEaten(json.has("itemsEaten") ? json.get("itemsEaten").getAsInt() : 0);
        stats.setDistanceTraveled(
                json.has("distanceTraveled") ? json.get("distanceTraveled").getAsDouble() : 0);
        stats.setBlocksBroken(json.has("blocksBroken") ? json.get("blocksBroken").getAsInt() : 0);
        stats.setDeaths(json.has("deaths") ? json.get("deaths").getAsInt() : 0);
        stats.setItemsCrafted(
                json.has("itemsCrafted") ? json.get("itemsCrafted").getAsInt() : 0);
        stats.setItemsUsed(json.has("itemsUsed") ? json.get("itemsUsed").getAsInt() : 0);
        stats.setChestsOpened(
                json.has("chestsOpened") ? json.get("chestsOpened").getAsInt() : 0);
        stats.setMessagesSent(
                json.has("messagesSent") ? json.get("messagesSent").getAsInt() : 0);
        return stats;
    }
}
