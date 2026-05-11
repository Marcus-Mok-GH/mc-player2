package com.orbitron.companion.chat;

import com.orbitron.companion.OrbitronCompanionMod;
import com.orbitron.companion.entity.CompanionEntity;
import com.orbitron.companion.network.BackendClient;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChatListener {
    private static final String TRIGGER_PREFIX = "@orbitron";
    private static final double COMPANION_NEARBY_RADIUS = 32.0;
    private static final String AI_PREFIX = "[Orbitron] ";

    private final ChatHandler chatHandler;

    public ChatListener() {
        this.chatHandler = new ChatHandler();
    }

    public void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String content = message.getContent().getString();
            OrbitronCompanionMod.LOGGER.info("Chat message from {}: {}", sender.getName().getString(), content);

            if (!shouldRespond(content, sender)) {
                return;
            }

            String playerName = sender.getName().getString();
            String payload = playerName + ": " + content;

            MinecraftServer server = sender.getServer();
            if (server == null) {
                OrbitronCompanionMod.LOGGER.warn("No server available for chat response");
                return;
            }

            OrbitronCompanionMod.LOGGER.info("Sending chat to backend: {}", payload);

            new Thread(() -> {
                String response = chatHandler.handleMessage(payload);
                OrbitronCompanionMod.LOGGER.info("Backend response: {}", response);

                server.execute(() -> {
                    Text formattedResponse = Text.literal(AI_PREFIX)
                        .formatted(Formatting.GOLD)
                        .append(Text.literal(response).formatted(Formatting.WHITE));

                    server.getPlayerManager().broadcast(formattedResponse, false);
                });
            }).start();
        });

        OrbitronCompanionMod.LOGGER.info("ChatListener registered");
    }

    private boolean shouldRespond(String content, ServerPlayerEntity sender) {
        if (content.toLowerCase().startsWith(TRIGGER_PREFIX)) {
            return true;
        }

        return hasCompanionNearby(sender);
    }

    private boolean hasCompanionNearby(ServerPlayerEntity player) {
        for (Entity entity : player.getWorld().getEntitiesByClass(CompanionEntity.class, player.getBoundingBox().expand(COMPANION_NEARBY_RADIUS), e -> true)) {
            if (entity instanceof CompanionEntity companion) {
                if (companion.getOwner() == player) {
                    return true;
                }
            }
        }
        return false;
    }
}
