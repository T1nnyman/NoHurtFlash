package mod.tinnyman.nohurtflash;

import mod.tinnyman.nohurtflash.mixin.EntityAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Nohurtflash.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GlowEffectHandler {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!ModConfig.ENABLE_GLOW.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        Scoreboard scoreboard = mc.level.getScoreboard();
        String teamName = "nohurtflash_glow";
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);

        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            ChatFormatting formatting = ModConfig.getGlowColor();
            team.setColor(formatting != null ? formatting : ChatFormatting.RED);
            team.setSeeFriendlyInvisibles(false);
            team.setAllowFriendlyFire(true);
        }

        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living) {
                boolean shouldGlow = living.hurtTime > 0;
                EntityDataAccessor<Byte> flagsId = EntityAccessor.getSharedFlagsId();
                byte currentFlags = living.getEntityData().get(flagsId);
                boolean isGlowing = (currentFlags & 0x40) != 0;
                String uuid = living.getStringUUID();
                PlayerTeam currentTeam = scoreboard.getPlayersTeam(uuid);

                if (shouldGlow && !isGlowing) {
                    living.getEntityData().set(flagsId, (byte)(currentFlags | 0x40));
                    if (currentTeam != team) {
                        scoreboard.addPlayerToTeam(uuid, team);
                    }
                } else if (!shouldGlow && isGlowing) {
                    living.getEntityData().set(flagsId, (byte)(currentFlags & ~0x40));
                    if (currentTeam == team) {
                        scoreboard.removePlayerFromTeam(uuid, team);
                    }
                }
            }
        }
    }
}
