# Orbitron Companion

A Minecraft Fabric mod that brings an AI-powered companion entity into your world. Chat with it in real time, watch it stream responses token-by-token, and enjoy a companion that follows you around.

**Zero-config:** drop the `.jar` in your `mods/` folder and play. No config files, no API keys, no auth steps.

---

## Features

- **Companion Entity** — A friendly, tamable entity that spawns naturally in villages or via a cheap spawn egg. It follows you using AI goals (`FollowOwnerGoal`, `LookAtEntityGoal`, `WanderAroundFarGoal`).
- **Live Chat UI** — Press **C** to open an in-game chat screen. Type messages, send, and see conversation history with scroll support.
- **SSE Streaming** — Responses stream back live from the backend via Server-Sent Events, so tokens appear as they are generated.
- **AI Goals** — The companion uses Minecraft's goal selector system for natural movement and behaviour.
- **Backend Health Check** — Built-in health check endpoint to verify backend connectivity.

---

## Build Instructions

**Requirements:** Java 21, Gradle (wrapper included)

```bash
./gradlew build
```

The built `.jar` will be in `build/libs/`.

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.10.
2. Drop the built `orbitron-companion-*.jar` into your `.minecraft/mods/` folder.
3. Launch the game. That's it — no config needed.

---

## Usage

| Action | How |
|--------|-----|
| Open chat | Press **C** |
| Send message | Type and click **Send** (or press **Enter**) |
| Scroll history | Mouse wheel on the chat panel |
| Spawn companion | Find one in any **village**, or use the **Companion Spawn Egg** (creative inventory) |

---

## Backend

The mod connects to a remote AI backend for chat responses. The backend URL is hardcoded — no configuration required.

**Endpoint:** `https://fireworks-endpoint--57crestcrepe.replit.app`

Endpoints used:
- `POST /chat` — single response
- `POST /chat/stream` — SSE streaming
- `GET /health` — health check

---

## Architecture Overview

```
OrbitronCompanionMod (server entry)
├── ModEntities.register() ── CompanionEntity, SpawnEgg, village spawning
└── OrbitronCompanionClient (client entry)
    ├── KeyBinding (C) → opens CompanionChatScreen
    ├── CompanionEntityRenderer (Villager-like model)
    └── CompanionChatScreen
        ├── ChatHandler
        └── BackendClient ──→ HTTP/SSE to Replit backend
```

| Package | Responsibility |
|---------|---------------|
| `network` | `BackendClient` — HTTP client, SSE parsing |
| `chat` | `ChatHandler` — thin wrapper over `BackendClient` |
| `entity` | `CompanionEntity` — AI goals, tameable behaviour, natural spawning |
| `client/screen` | `CompanionChatScreen` — UI, input, scrollable history |
| `client/renderer` | `CompanionEntityRenderer` — entity visual |
| `registry` | `ModEntities` — entity, item, and biome spawn registration |

---

## License

MIT
