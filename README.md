# RoyaSocialBots 1.3.0 — Paper 1.21.11

Compiled with Java 21 against the Paper 1.21.11 API supplied in the user's Library. This package adds the automatic Minecraft resource-pack prompt on player join.

## Install
1. Put `RoyaSocialBots-1.3.0.jar` in the server's `plugins/` folder and restart.
2. Host `ROYA-SocialBots-3D-ResourcePack-1.21.11.zip` at a **public direct-download HTTPS URL**. A normal webpage or private/permissioned link will not work. Minecraft clients must be able to download the ZIP directly.
3. Edit `plugins/RoyaSocialBots/config.yml` and set `resource-pack.url` to that direct URL. The `sha1` is already set for the included ZIP; if you modify/recompress the ZIP, calculate its new SHA-1 and replace the value.
4. On join, players will automatically receive the resource-pack prompt. `resource-pack.required: false` lets them decline; change it to `true` if you want the pack required.
5. Set bot locations and links using `/socialbots set discord`, `/socialbots set youtube`, `/socialbots link discord <url>`, and `/socialbots link youtube <url>`.

## Important limitation
The plugin sends the built-in Minecraft resource-pack request automatically, but it cannot make the ZIP reachable from nowhere. You must host the pack at a public direct-download URL; Paper/game-server hosting does not automatically provide an HTTP file-hosting URL.

The 3D item models are supplied by the companion resource pack. Without accepting/installing it, custom mascot models will not display correctly. Test on a backup server before using on the main server.
