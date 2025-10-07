package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MSTimer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(
        name = "AntiBots",
        category = Category.COMBAT,
        description = "Prevents bots from attacking you"
)
public class AntiBots extends Module {
    private static final Map<UUID, String> uuidDisplayNames = new ConcurrentHashMap<>();
    private static final Map<Integer, String> entityIdDisplayNames = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> uuids = new ConcurrentHashMap<>();
    static final Set<Integer> ids = new HashSet<>();
    private static final Map<UUID, Long> respawnTime = new ConcurrentHashMap<>();
    private final FloatValue respawnTimeValue = ValueBuilder.create(this, "Respawn Time")
            .setDefaultFloatValue(2500.0F)
            .setFloatStep(100.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10000.0F)
            .build()
            .getFloatValue();
    public static final List<String> botName = new CopyOnWriteArrayList<>();
    private final BooleanValue HeypixelCombat = ValueBuilder.create(this, "HeypixelCombat")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final BooleanValue HeypixelNewBot = ValueBuilder.create(this, "HeypixelNewBot")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final BooleanValue HeypixelNewCombat = ValueBuilder.create(this, "HeypixelNewCombat")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final BooleanValue HeypixelStaff = ValueBuilder.create(this, "HeypixelStaff")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final BooleanValue HeypixelNPC = ValueBuilder.create(this, "HeypixelNPC")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    private final BooleanValue HytDeadPlayer = ValueBuilder.create(this, "HytDeadPlayer")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    private final BooleanValue livingTimeValue = ValueBuilder.create(this, "LivingTime")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    private final FloatValue livingTimeTicksValue = ValueBuilder.create(this, "LivingTimeTicks")
            .setDefaultFloatValue(40.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(200.0F)
            .build()
            .getFloatValue();
    private final BooleanValue ChatDeBug = ValueBuilder.create(this, "ChatDeBug")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final MSTimer timer = new MSTimer();
    boolean isStaff = false;
    public static boolean isBedWarsBot(Entity entity) {
        AntiBots module = (AntiBots)Naven.getInstance().getModuleManager().getModule(AntiBots.class);
        if (module.respawnTimeValue.getCurrentValue() < 1.0F) {
            return false;
        } else {
            return !respawnTime.containsKey(entity.getUUID())
                    ? false
                    : (float)(System.currentTimeMillis() - respawnTime.get(entity.getUUID())) < module.respawnTimeValue.getCurrentValue();
        }
    }

    public static boolean isBot(Entity entity) {
        return ids.contains(entity.getId());
    }

    public static boolean isBotEntity(Entity entity) {
        return isBot(entity) || isBedWarsBot(entity);
    }

    public static boolean isXPowerBot(Entity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (player == mc.player) {
                return false;
            }
            return botName.contains(player.getName().getString());
        }
        return false;
    }

    public static boolean isAnyBot(Entity entity) {
        if (entity == mc.player) {
            return false;
        }
        return isBotEntity(entity) || isXPowerBot(entity);
    }

    @EventTarget
    public void bedWarsBot(EventPacket e) {
        if (e.getType() == EventType.RECEIVE && mc.level != null) {
            if (e.getPacket() instanceof ClientboundPlayerInfoUpdatePacket) {
                ClientboundPlayerInfoUpdatePacket packet = (ClientboundPlayerInfoUpdatePacket)e.getPacket();
                if (packet.actions().contains(Action.ADD_PLAYER)) {
                    for (net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
                        GameProfile profile = entry.profile();
                        UUID id = profile.getId();
                        respawnTime.put(id, System.currentTimeMillis());
                    }
                }
            } else if (e.getPacket() instanceof ClientboundAnimatePacket) {
                ClientboundAnimatePacket packet = (ClientboundAnimatePacket)e.getPacket();
                Entity entity = mc.level.getEntity(packet.getId());
                if (entity != null && packet.getAction() == 0 && respawnTime.containsKey(entity.getUUID())) {
                    respawnTime.remove(entity.getUUID());
                }
            }
        }
    }

    @EventTarget
    public void onRespawn(EventRespawn e) {
        uuidDisplayNames.clear();
        entityIdDisplayNames.clear();
        ids.clear();
        uuids.clear();
        botName.clear();
        isStaff = false;
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            for (Entry<UUID, Long> entry : uuids.entrySet()) {
                if (System.currentTimeMillis() - entry.getValue() > 500L) {
                    ChatUtils.addChatMessage("Fake Staff Detected! (" + uuidDisplayNames.get(entry.getKey()) + ")");
                    uuids.remove(entry.getKey());
                }
            }

            if (mc.player != null && mc.level != null) {
                if (this.HeypixelCombat.getCurrentValue()) {
                    Map<String, Integer> map = new HashMap<>();
                    for (net.minecraft.client.multiplayer.PlayerInfo playerinfo : mc.player.connection.getOnlinePlayers()) {
                        String s = playerinfo.getProfile().getName();
                        if (s.equals(mc.player.getName().getString())) {
                            continue;
                        }
                        map.put(s, map.getOrDefault(s, 0) + 1);
                    }

                    for (String s2 : map.keySet()) {
                        if (map.get(s2) > 1 && !botName.contains(s2)) {
                            botName.add(s2);
                            if (this.ChatDeBug.getCurrentValue()) {
                                ChatUtils.addChatMessage("Check a Combat Bot! (BotName :" + s2 + ")");
                            }
                        }
                    }

                    botName.removeIf(name -> !map.containsKey(name) || map.get(name) == 1);
                }

                if (this.HeypixelStaff.getCurrentValue()) {
                    String[] astring = new String[]{"绿豆乃SAMA", "nightbary", "体贴的炼金术雀", "StarNO1", "妖猫", "小妖猫", "妖猫的PC号", "小H修bug", "xiaotufei", "元宵", "CuteGirlQlQl", "彩笔", "布吉岛打工仔", "元宵的测试号", "抑郁的元宵", "元宵睡不醒", "抖音丶小匪", "练书法的苦力怕", "KiKiAman", "元宵睡不醒", "WS故", "彩笔qwq", "管理员-1", "管理员-2", "管理员-3", "管理员-4", "管理员-5", "管理员-6", "管理员-7", "管理员-8", "管理员-9", "管理员-10", "天使", "艾米丽", "可比不来嗯忑", "鸡你太美", "神伦子", "马哥乐"};

                    List<String> list1 = new CopyOnWriteArrayList<>();

                    for (net.minecraft.client.multiplayer.PlayerInfo playerinfo2 : mc.player.connection.getOnlinePlayers()) {
                        String playerName = playerinfo2.getProfile().getName();
                        if (!playerName.equals(mc.player.getName().getString())) {
                            list1.add(playerName);
                        }
                    }

                    for (String s1 : astring) {
                        if (list1.contains(s1) && !this.isStaff) {
                            this.isStaff = true;
                            if (this.ChatDeBug.getCurrentValue()) {
                                ChatUtils.addChatMessage("Check a Staff Bot! (BotName :" + s1 + ")");
                            }
                        }
                    }
                }

                if (this.HeypixelNPC.getCurrentValue()) {
                    for (Player player1 : mc.level.players()) {
                        if (player1 == mc.player) {
                            continue;
                        }
                        String s4 = player1.getName().getString();
                        if (s4.startsWith("CIT-")) {
                            botName.add(s4);
                        }
                    }
                }

                if (this.HeypixelNewCombat.getCurrentValue()) {
                    for (Player player2 : mc.level.players()) {
                        if (player2 == mc.player) {
                            continue;
                        }
                        String s5 = player2.getName().getString();
                        if (botName.contains(s5)) {
                            continue;
                        }

                        if (player2.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                                && player2.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                                && player2.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                                && player2.getItemBySlot(EquipmentSlot.FEET).isEmpty()
                                && player2.hasEffect(MobEffects.INVISIBILITY)) {
                            botName.add(s5);
                            if (this.ChatDeBug.getCurrentValue()) {
                                ChatUtils.addChatMessage("Check a NewCombat Bot! (BotName :" + s5 + ")");
                            }
                        }

                        if (player2.getItemBySlot(EquipmentSlot.HEAD).is(Items.GOLDEN_HELMET) ||
                                player2.getItemBySlot(EquipmentSlot.CHEST).is(Items.GOLDEN_CHESTPLATE)) {
                            botName.add(s5);
                            if (this.ChatDeBug.getCurrentValue()) {
                                ChatUtils.addChatMessage("Check a NewCombat Bot! (BotName :" + s5 + ")");
                            }
                        }
                    }
                }

                if (this.HytDeadPlayer.getCurrentValue()) {
                    List<String> list = new ArrayList<>();

                    for (Player player4 : mc.level.players()) {
                        if (player4 == mc.player) {
                            continue;
                        }
                        if (player4.isSleeping() || player4.getEyeHeight() <= 0.5F || player4.tickCount < 25) {
                            list.add(player4.getName().getString());
                        }
                    }

                    botName.addAll(list);
                    List<String> list2 = new ArrayList<>();

                    for (String s6 : botName) {
                        boolean flag = false;

                        for (Player player : mc.level.players()) {
                            if (player == mc.player) {
                                continue;
                            }
                            if (player.getName().getString().equals(s6)
                                    && (player.isSleeping() || player.getEyeHeight() <= 0.5F || player.tickCount < (int)this.livingTimeTicksValue.getCurrentValue())) {
                                flag = true;
                                break;
                            }
                        }

                        if (!flag) {
                            list2.add(s6);
                        }
                    }

                    botName.removeAll(list2);
                }

                if (this.livingTimeValue.getCurrentValue()) {
                    for (Player player3 : mc.level.players()) {
                        if (player3 == mc.player) {
                            continue;
                        }
                        if (player3.tickCount < (int)this.livingTimeTicksValue.getCurrentValue()) {
                            if (!botName.contains(player3.getName().getString())) {
                                botName.add(player3.getName().getString());
                            }
                        } else {
                            botName.remove(player3.getName().getString());
                        }
                    }
                }

                if (this.isStaff && this.timer.hasTimePassed(30000L)) {
                    this.isStaff = false;
                    this.timer.reset();
                }
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getType() == EventType.RECEIVE) {
            if (e.getPacket() instanceof ClientboundPlayerInfoUpdatePacket) {
                ClientboundPlayerInfoUpdatePacket packet = (ClientboundPlayerInfoUpdatePacket)e.getPacket();
                if (packet.actions().contains(Action.ADD_PLAYER)) {
                    for (net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
                        if (entry.displayName() != null && entry.displayName().getSiblings().isEmpty() && entry.gameMode() == GameType.SURVIVAL) {
                            UUID uuid = entry.profile().getId();
                            if (!uuid.equals(mc.player.getUUID())) {
                                uuids.put(uuid, System.currentTimeMillis());
                                uuidDisplayNames.put(uuid, entry.displayName().getString());
                            }
                        }
                    }
                }
            } else if (e.getPacket() instanceof ClientboundAddPlayerPacket) {
                ClientboundAddPlayerPacket packet = (ClientboundAddPlayerPacket)e.getPacket();
                if (!packet.getPlayerId().equals(mc.player.getUUID()) && uuids.containsKey(packet.getPlayerId())) {
                    String displayName = uuidDisplayNames.get(packet.getPlayerId());
                    ChatUtils.addChatMessage("Bot Detected! (" + displayName + ")");
                    entityIdDisplayNames.put(packet.getEntityId(), displayName);
                    uuids.remove(packet.getPlayerId());
                    ids.add(packet.getEntityId());
                }
            } else if (e.getPacket() instanceof ClientboundRemoveEntitiesPacket) {
                ClientboundRemoveEntitiesPacket packet = (ClientboundRemoveEntitiesPacket)e.getPacket();
                IntListIterator var9 = packet.getEntityIds().iterator();

                while (var9.hasNext()) {
                    Integer entityId = (Integer)var9.next();
                    if (ids.contains(entityId)) {
                        String displayName = entityIdDisplayNames.get(entityId);
                        ChatUtils.addChatMessage("Bot Removed! (" + displayName + ")");
                        ids.remove(entityId);
                    }
                }
            }
        }
    }
}