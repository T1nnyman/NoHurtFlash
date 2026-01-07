package net.tinnyman.nohurtflash;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tinnyman.nohurtflash.mixin.EntityAccessor;

@Mod.EventBusSubscriber(modid = NoHurtFlash.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlowEffectHandler {
    private static final String TEAM_NAME = "nohurtflash_glow";
    private static final int GLOW_FLAG_BIT = 0x40; // bit 6

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!ModConfig.ENABLE_GLOW.get()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        if (mc.isPaused()) return;

        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_NAME);
            ChatFormatting color = ModConfig.getGlowColor();
            team.setColor(color != null ? color : ChatFormatting.RED);
            team.setAllowFriendlyFire(false);
            team.setSeeFriendlyInvisibles(true);
        }

        EntityDataAccessor<Byte> sharedFlagsId = EntityAccessor.getSharedFlagsId();

        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (living instanceof Player) continue;

            boolean shouldGlow = living.hurtTime > 0 && VisibilityUtil.canPlayerSeeEntityNow(living);
            byte flags = living.getEntityData().get(sharedFlagsId);
            boolean isGlowingNow = (flags & GLOW_FLAG_BIT) != 0;
            String scoreboardName = living.getScoreboardName();
            PlayerTeam currentTeam = scoreboard.getPlayersTeam(scoreboardName);

            if (shouldGlow) {
                if (!isGlowingNow) {
                    living.getEntityData().set(sharedFlagsId, (byte)(flags | GLOW_FLAG_BIT));
                }
                if (currentTeam != team) {
                    scoreboard.addPlayerToTeam(scoreboardName, team);
                }
            } else {
                if (isGlowingNow) {
                    living.getEntityData().set(sharedFlagsId, (byte)(flags & ~GLOW_FLAG_BIT));
                }
                if (currentTeam == team) {
                    scoreboard.removePlayerFromTeam(scoreboardName, team);
                }
            }
        }
    }

    public static void setGlowing(LivingEntity entity, boolean glowing) {
        EntityDataAccessor<Byte> FLAGS = EntityAccessor.getSharedFlagsId();
        byte flags = entity.getEntityData().get(FLAGS);

        if (glowing) flags |= GLOW_FLAG_BIT;
        else flags &= ~GLOW_FLAG_BIT;

        entity.getEntityData().set(FLAGS, flags);
    }
}
