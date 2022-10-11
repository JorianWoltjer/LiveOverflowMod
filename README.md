# LiveOverflowMod

A Minecraft hacked-client for the LiveOverflow Server. Contains various hacks for the challenges on the server,
and some utilities. 

**Read all about it in my [blog post](https://jorianwoltjer.com/blog/)**.

## Hacks

### Anti-Human Bypass

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

### WorldGuard Bypass

WorldGuard was used to deny `entry` to a protected area, in which the player had to die in the lava to complete the challenge. 
WorldGuard works using `PlayerMoveEvent`s and this bypass works by moving without triggering this event.  

Only when movement is large enough, an event is sent. So we can move a small amount, and then save the position by "moving too quickly!". 
This means it can only move `0.06` blocks per tick, and then has to send a position packet far away to trigger the warning and reset your position
for the next repeat. However, this can be improved because WorldGuard only checks regions when you cross a block boundary. 
So when we can move almost a full block while not crossing the boundary, and then only move a small amount to cross the boundary. 

When this hack is activated using the default `;` (semicolon) keybind, it will allow you to move in this way with your `WASD` keys. 

* [Keybinds.java](src/main/java/com/jorianwoltjer/liveoverflowmod/client/Keybinds.java):
When the keybind is pressed, `worldGuardBypassEnabled` is activated and `WASD` keys send the required packets to bypass WorldGuard
* [LivingEntityMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/LivingEntityMixin.java):
Redirect the `isImmobile()` method to return true when the hack is enabled, so the normal player movement is disabled

### Reach

While reading the movement code, I found that it is possible to send a position packet a maximum of `10` blocks away from the current position.
But also that you can send `5` packets per tick, so you can move 50 blocks in a single tick. This gave me the idea of making
a reach hack that uses this by sending multiple position packets going towards a player, hit them, and then move back. 
That is exactly what this hack does to the nearest player when pressing the default `\​` (backslash) keybind.

> **Note**
> This hack is not perfect by any means. It only works when there is a clear line of sight to the player, and sometimes gets
> stuck while moving meaning you end up somewhere along traveled path. But it's good enough for a proof of concept!

* [KeyBinds.java](src/main/java/com/jorianwoltjer/liveoverflowmod/client/Keybinds.java):
When the keybind is pressed, it will find the nearest player, then calculate and queue a few packets to move towards them.
Then it will send a packet to hit them, and then a few packets to move back to the original position. Sending the packets 
may take multiple ticks, so these are remembered and sent when the next tick happens.
* [ClientPlayerEntityMixin.java](src/main/java/com/jorianwoltjer/liveoverflowmod/mixin/ClientPlayerEntityMixin.java):
Cancel the `sendMovementPackets()` method when this hack is traveling, so the client doesn't send any wrong packets by itself

### Panic

This hack is more of a test to see what is possible with movement. It sends 5 packets per tick each moving you up 10 blocks.
It does so 1 full second meaning you end up traveling precisely 1000 blocks straight up. This is useful if there is some danger, 
like someone trying to kill you. You can then press the default `,` (comma) keybind to go up 1000 blocks and get out of the danger, 
maybe even logging off at that high position because you cannot be trapped with blocks up there. 

* [KeyBinds.java](src/main/java/com/jorianwoltjer/liveoverflowmod/client/Keybinds.java):
When the keybind is pressed, it will send 5 packets going up 10 blocks each time for 20 ticks in total

### Insta-Mine

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

## Development

### Building to JAR (IntelliJ)

1. Select `liveoverflowmod build` task and click green arrow in
2. JAR in [build/libs](build/libs)

### Building and running Minecraft (IntelliJ)

1. Select `Minecraft Client` task and click green arrow
2. Minecraft will start, with logs in IntelliJ

## Resources

* An example of a simple fabric mod with a Mixin: https://github.com/FabricMC/fabric-example-mod
* Getting the Paper-Server source code: https://github.com/PaperMC/Paper/blob/master/CONTRIBUTING.md
