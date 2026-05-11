package com.orbitron.companion.chat;

import com.orbitron.companion.OrbitronCompanionMod;
import com.orbitron.companion.ai.ToolExecutor;
import com.orbitron.companion.entity.CompanionEntity;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

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

            CompanionEntity companion = findNearestCompanion(sender);
            if (companion == null) {
                OrbitronCompanionMod.LOGGER.warn("No companion found for player: {}", playerName);
            }

            OrbitronCompanionMod.LOGGER.info("Sending chat to backend: {}", payload);

            server.execute(() -> {
                final CompanionEntity finalCompanion = companion;
                final String finalPlayerName = playerName;

                new Thread(() -> {
                    try {
                        String response = chatHandler.handleMessage(payload);
                        OrbitronCompanionMod.LOGGER.info("Backend response: {}", response);

                        String displayText = response;
                        boolean executedTools = false;

                        if (finalCompanion != null) {
                            server.execute(() -> {
                                ToolExecutor.ToolResult result = ToolExecutor.parseAndExecute(finalCompanion, response);
                                final String toolDisplayText = result.responseText;
                                final boolean toolExecuted = result.executedAnyTool;

                                server.execute(() -> {
                                    Text formattedResponse = Text.literal(AI_PREFIX)
                                        .formatted(Formatting.GOLD)
                                        .append(Text.literal(toolDisplayText).formatted(Formatting.WHITE));

                                    server.getPlayerManager().broadcast(formattedResponse, false);

                                    if (toolExecuted) {
                                        OrbitronCompanionMod.LOGGER.info("Tools executed for companion of player: {}", finalPlayerName);
                                    }
                                });
                            });
                        } else {
                            server.execute(() -> {
                                Text formattedResponse = Text.literal(AI_PREFIX)
                                    .formatted(Formatting.GOLD)
                                    .append(Text.literal(displayText).formatted(Formatting.WHITE));

                                server.getPlayerManager().broadcast(formattedResponse, false);
                            });
                        }
                    } catch (Exception e) {
                        OrbitronCompanionMod.LOGGER.error("Failed to process chat message", e);
                        server.execute(() -> {
                            Text errorText = Text.literal(AI_PREFIX)
                                .formatted(Formatting.GOLD)
                                .append(Text.literal("Error processing request").formatted(Formatting.RED));
                            server.getPlayerManager().broadcast(errorText, false);
                        });
                    }
                }).start();
            });
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
        World world = player.getWorld();
        if (world == null) {
            OrbitronCompanionMod.LOGGER.warn("Player world is null for: {}", player.getName().getString());
            return false;
        }

        for (Entity entity : world.getEntitiesByClass(CompanionEntity.class, player.getBoundingBox().expand(COMPANION_NEARBY_RADIUS), e -> true)) {
            if (entity instanceof CompanionEntity companion) {
                if (companion.getOwner() != null && companion.getOwner() == player) {
                    return true;
                }
            }
        }
        return false;
    }

    private CompanionEntity findNearestCompanion(ServerPlayerEntity player) {
        World world = player.getWorld();
        if (world == null) {
            OrbitronCompanionMod.LOGGER.warn("Player world is null for: {}", player.getName().getString());
            return null;
        }

        CompanionEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity entity : world.getEntitiesByClass(CompanionEntity.class, player.getBoundingBox().expand(COMPANION_NEARBY_RADIUS), e -> true)) {
            if (entity instanceof CompanionEntity companion) {
                if (companion.getOwner() != null && companion.getOwner() == player) {
                    double dist = player.squaredDistanceTo(companion);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = companion;
                    }
                }
            }
        }

        return nearest;
    }
}
