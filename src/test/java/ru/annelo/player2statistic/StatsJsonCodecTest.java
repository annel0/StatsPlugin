package ru.annelo.player2statistic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class StatsJsonCodecTest {
    @Test
    void roundTripPreservesStats() {
        PlayerStats stats = new PlayerStats();
        UUID uuid = UUID.randomUUID();
        stats.setUuid(uuid);
        stats.setPlayerName("PlayerOne");
        stats.setPlayTime(42);
        stats.setMobsKilled(3);
        stats.setItemsEaten(5);
        stats.setDistanceTraveled(12.5);
        stats.setBlocksBroken(7);
        stats.setDeaths(2);
        stats.setItemsCrafted(4);
        stats.setItemsUsed(6);
        stats.setChestsOpened(8);
        stats.setMessagesSent(9);

        JsonObject json = StatsJsonCodec.toJson(stats);
        PlayerStats parsed = StatsJsonCodec.fromJson(json, uuid);

        assertEquals(uuid, parsed.getUuid());
        assertEquals("PlayerOne", parsed.getPlayerName());
        assertEquals(42, parsed.getPlayTime());
        assertEquals(3, parsed.getMobsKilled());
        assertEquals(5, parsed.getItemsEaten());
        assertEquals(12.5, parsed.getDistanceTraveled());
        assertEquals(7, parsed.getBlocksBroken());
        assertEquals(2, parsed.getDeaths());
        assertEquals(4, parsed.getItemsCrafted());
        assertEquals(6, parsed.getItemsUsed());
        assertEquals(8, parsed.getChestsOpened());
        assertEquals(9, parsed.getMessagesSent());
    }

    @Test
    void fromJsonUsesFallbackWhenMissingUuid() {
        UUID fallback = UUID.randomUUID();
        JsonObject json = new JsonObject();
        json.addProperty("player_name", "FallbackPlayer");
        json.addProperty("playTime", 10);

        PlayerStats parsed = StatsJsonCodec.fromJson(json, fallback);

        assertEquals(fallback, parsed.getUuid());
        assertEquals("FallbackPlayer", parsed.getPlayerName());
        assertEquals(10, parsed.getPlayTime());
    }

    @Test
    void toJsonIncludesUuid() {
        PlayerStats stats = new PlayerStats();
        UUID uuid = UUID.randomUUID();
        stats.setUuid(uuid);

        JsonObject json = StatsJsonCodec.toJson(stats);

        assertNotNull(json.get("uuid"));
        assertEquals(uuid.toString(), json.get("uuid").getAsString());
    }
}
