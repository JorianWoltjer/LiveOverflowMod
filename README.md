# LiveOverflowMod

A Minecraft hacked-client for the LiveOverflow Server. Contains various hacks for the challenges on the server,
and some utilities. 

**Read all about it in my [blog post](https://jorianwoltjer.com/blog/post/hacking/playing-on-the-liveoverflow-minecraft-hacking-server)**, and now also [**Part 2**](https://jorianwoltjer.com/blog/post/hacking/part-2-the-new-liveoverflow-minecraft-hacking-server)!

## Hacks

### WorldGuard Bypass <kbd>;</kbd>

WorldGuard was used to deny `entry` to a protected area, in which the player had to die in the lava to complete the challenge. 
WorldGuard works using `PlayerMoveEvent`s and this bypass works by moving without triggering this event.  

Only when movement is large enough, an event is sent. So we can move a small amount, and then save the position by "moving too quickly!". 
This means it can only move `0.06` blocks per tick, and then has to send a position packet far away to trigger the warning and reset your position
for the next repeat. However, this can be improved because WorldGuard only checks regions when you cross a block boundary. 
So when we can move almost a full block while not crossing the boundary, and then only move a small amount to cross the boundary. 

When this hack is activated using the default <kbd>;</kbd> (semicolon) keybind, it will allow you to move in this way with your `WASD` keys. 

[**YouTube Video - Showcase**](https://www.youtube.com/watch?v=hYA1cTUOXgA)

* [WorldGuardBypass.java](src/main/java/com/jorianwoltjer/liveoverflowmod/hacks/WorldGuardBypass.java):
When activated, it performs the bypass explained above until it is deactivated again
* [LivingEntityMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/LivingEntityMixin.java):
Redirect the `isImmobile()` method to return true when the hack is enabled, so the normal player movement is disabled

### Reach <kbd>]</kbd>

While reading the movement code, I found that it is possible to send a position packet a maximum of `10` blocks away from the current position.
But also that you can send `5` packets per tick, so you can move 50 blocks in a single tick. This gave me the idea of making
a reach hack that uses this by sending multiple position packets going towards a player, hit them, and then move back. 
That is exactly what this hack does when you toggle the default <kbd>]</kbd> (right bracket) keybind, and then click on a far away entity as
if you were hitting them. 

> **Warning**:
> This hack is not perfect. It only works when there is a clear line of sight to the player, and sometimes gets
> stuck while moving meaning you end up somewhere along traveled path. But it's good enough for a proof of concept!  
> See [Clip Reach](#clip-reach) for a better version of this hack that teleports through blocks.

[**YouTube Video - Showcase**](https://www.youtube.com/watch?v=Hio_iDnnJ5c)

* [Reach.java](src/main/java/com/jorianwoltjer/liveoverflowmod/hacks/Reach.java):
When enabled, you will teleport to every entity you click from a distance, and then teleported back to your original position
* [ClientPlayerInteractionManagerMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/ClientPlayerInteractionManagerMixin.java):
  Detect clicking on entities and forward it to the teleport function
* [MinecraftClientMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/MinecraftClientMixin.java):
  When enabled, the client will think it can hit entities from far away to make the Minecraft UI work correctly and detect hit attempts

### Clip Reach <kbd>[</kbd>

There is a trick to clip huge distances by sending many movement packets in a single tick, and then doing one big jump. 
This is implemented into the Clip Reach hack which when enabled using the default <kbd>[</kbd> (left bracket) keybind, will
use this to teleport upwards, then above the target, and finally down to the target. Afterward it goes back up, to your 
original position, and back down. This all happens without seeing any teleports on screen, for a clean Reach feeling.

> **Warning**:
> Due to the required number of packets needed to be sent in a single tick, it may fail sometimes and get you stuck somewhere 
> in the middle, often in the air. Because of this it is recommended to use a NoFall hack when trying to use this hack so you
> don't die :)

[**YouTube Video - Showcase**](https://www.youtube.com/watch?v=_eBEtPoCuj0)

* [ClipReach.java](src/main/java/com/jorianwoltjer/liveoverflowmod/hacks/ClipReach.java):
When enabled, you will teleport to every entity you click from a distance, through walls, and then teleported back to your original position
* [ClientPlayerInteractionManagerMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/ClientPlayerInteractionManagerMixin.java):
Detect clicking on entities and forward it to the teleport function
* [MinecraftClientMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/MinecraftClientMixin.java):
When enabled, the client will think it can hit entities from far away to make the Minecraft UI work correctly and detect hit attempts

### Panic Mode <kbd>,</kbd>

When you want to AFK, you can enable this mode with the <kbd>,</kbd> (comma) keybind to make sure nothing happens to you. It detects players entering your render 
distance, and receiving any form of damage. When it detects any of these, it sends 5 packets per tick each moving you up 10 blocks.
It does so 1 full second meaning you end up traveling precisely 1000 blocks straight up. Afterward it instantly disconnects you from the server. 

* [PanicMode.java](src/main/java/com/jorianwoltjer/liveoverflowmod/hacks/PanicMode.java)
Handles detecting players entering your render distance, and the teleportation with disconnecting itself
* [ClientPlayNetworkHandlerMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/ClientPlayNetworkHandlerMixin.java#L50-L55):
When you take any damage, also trigger

### Passive Mods <kbd>-</kbd>

Passive Mods are enabled by default, and are expected to always be used. They are utility mods that don't really have a downside.

You can toggle all passive mods at once using the default <kbd>-</kbd> (minus) keybind.

* [PassiveMods.java](src/main/java/com/jorianwoltjer/liveoverflowmod/hacks/PassiveMods.java)

#### Anti-Human Bypass

All movement packets need to be rounded to the 100ths. This is done using a simple `Math.round()` function,
and to fix floating point errors the `Math.nextAfter()` function is used.

* [RoundPosition.java](src/main/java/com/jorianwoltjer/liveoverflowmod/helper/RoundPosition.java):
  Does the rounding calculations
* [PlayerPositionFullPacketMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/PlayerPositionFullPacketMixin.java):
  Intercept the `PlayerMoveC2SPacket.Full` and round the position when sending it to the server
* [PlayerPositionPacketMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/PlayerPositionPacketMixin.java):
  Same as above, but for the `PlayerMoveC2SPacket.PositionAndOnGround` packet
* [VehicleMovePacketMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/VehicleMovePacketMixin.java):
  Same as above, but for riding a vehicle like a boat

#### Disable Weird Packets

There are a few packets that the server tries to send to you that are actually fake, namely World Border, Creative Mode, Demo Mode and End Credits. 
These packets are not actually enforced by the server, only to mess with your client. This mod ignores them. 

> **Warning**:
> The mod does not differentiate between the server sending a fake gamemode change, or you actually changing gamemode. Therefore, 
> if you want to change gamemode, you need to disable this mod first.

* [ClientPlayNetworkHandlerMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/ClientPlayNetworkHandlerMixin.java):
Ignore or change packets as they come in

#### Insta-Mine

This hack is a simple one useful in some situations. When you try to break a block, it will send `START_DESTROY_BLOCK` action to the server. 
When you as the client are done breaking the block, it will send `STOP_DESTROY_BLOCK` to the server, and it will verify 
if you have been breaking the block for long enough. It turns out that the server only checks if you have broken the block
70% of the way, so you can send `STOP_DESTROY_BLOCK` a bit faster than normal. (see `ServerPlayerGameMode.java` in the Paper-Server source code)

```Java
public void handleBlockBreakAction(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int worldHeight, int sequence) {
    ...
    float f1 = iblockdata.getDestroyProgress(this.player, this.player.level,pos) * (float) (l+1);

    if(f1 >= 0.7F) {  // <--- This check is abused
        this.isDestroyingBlock = false;
        this.level.destroyBlockProgress(this.player.getId(), pos, -1);
        this.destroyAndAck(pos, sequence, "destroyed");  // Count as destroyed
        return;
    }
    ...
}
```

This allows you to insta-mine some blocks, that were previously really close to being insta-minable. You can for example insta-mine 
stone and cobblestone with an Efficiency 5 Netherite Pickaxe using this hack.

* [ClientPlayerInteractionMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/ClientPlayerInteractionMixin.java):
Send the `STOP_DESTROY_BLOCK` action packet right after starting to break a block if it is close enough to be insta-mined

#### Random Texture Rotations

This hack changed the random texture rotation code to use a completely random number every time, so it is not reversible from a screenshot. 
This was to be able to take screenshots and videos, while not revealing the texture rotations that can be brute-forced easily for coordinates. 
The same hack was used by LiveOverflow himself to make the challenge of finding his base harder. 

* [AbstractBlockMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/AbstractBlockMixin.java):
Use a randomized hash function to generate block rotations, making it unpredictable

### Commands

#### Clipping

A lot can be done with the ability to clip through walls. There are a few commands that do useful things like flipping 
vertically and horizontally. There are 4 common-use commands, and 2 specific ones for challenges:

* `/vclip`: Vertical clip, through ceilings and floors, up to around 180 blocks (always straight up)
* `/hclip`: Horizontal clip, through walls by teleporting up, forward and then down. Up to around 150 blocks (always flat)
* `/dclip`: Directional clip, into the exact direction you are looking. Up to around 150 blocks
* `/autoclip [up|down]`: Automatically clip through the nearest floor or ceiling, placing you at the first open air gap the player fits

* `/vault`: Complete the Vault challenge by clipping through the 50 blocks of bedrock. Start from the button and it will 
automatically start the challenge, teleport and finish the challenge giving you a mask
* `/clubmate`: This trick also works on the original Club Mate challenge, it will simply teleport to the chest and open it in one tick

* [ClipCommand.java](src/main/java/com/jorianwoltjer/liveoverflowmod/command/ClipCommand.java):
Includes all the logic for clipping through walls, as well as the registered commands

#### Bedrock Cracking

I made a few commands to aid in cracking the bedrock formation in order to find LiveOverflow's base:
* 
* `/bedrock`: Simply tell if a certain coordinate should be bedrock or not (in The Nether). It calculates this itself, 
not by looking at the block data in chunks. This command was useful because you can set a breakpoint in the code to see exactly
what the algorithm is doing, while comparing it to your own code.   
* `/getcode [from] [to]`: Select an area from two coordinates, and get the Rust code of the offsets from 
a recreation of bedrock copied to your clipboard. Simply recreate some formation with *bedrock* and *glass*, and run the command 
over the area to get the code. It is meant to be used with my [BedrockFinder](https://github.com/JorianWoltjer/BedrockFinder) tool, 
which will then find the coordinates of that bedrock formation in a specific seed.

* [GetCodeCommand.java](src/main/java/com/jorianwoltjer/liveoverflowmod/command/GetCodeCommand.java):
`/getcode` command and `/bedrock` command implementations

## Development

### Building to JAR (IntelliJ)

1. Select `liveoverflowmod build` task and click green arrow in
2. JAR in [build/libs](build/libs)

### Building and running Minecraft (IntelliJ)

1. Select `Minecraft Client` task and click green arrow
2. Minecraft will start, with logs in IntelliJ

> **Note**:
> Use the [ViaFabric](https://www.curseforge.com/minecraft/mc-mods/viafabric) mod to connect to any lower version server

## Resources

* An example of a simple fabric mod with a Mixin: https://github.com/FabricMC/fabric-example-mod
* Getting the Paper-Server source code: https://github.com/PaperMC/Paper/blob/master/CONTRIBUTING.md
