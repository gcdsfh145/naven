package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.protocol.YaoMaoFucker;
import com.heypixel.heypixelmod.obsoverlay.protocol.spoofer.FakeDiskStore;
import com.heypixel.heypixelmod.obsoverlay.protocol.spoofer.FakeMac;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jnic.JNICInclude;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;
import org.msgpack.value.Variable;
import oshi.hardware.Baseboard;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.swing.JOptionPane;

@JNICInclude
@ModuleInfo(
        name = "Protocol",
        description = "Fix you in NetEase's NetWork Server Protocol(Only Display,No Effect)",
        category = Category.MISC
)
public class Protocol extends Module {
    private static final String[] MODELS = new String[]{"i9", "i7", "i5", "i3"};
    private static final String[] SUB_MODELS = new String[]{
            "12900K", "12900KF", "12600K", "12600KF", "12400K", "12400KF", "9100F", "8100F", "10400F", "10400H", "11400F", "11700K"
    };
    private static final int[] FREQUENCIES = new int[]{2200, 2400, 2600, 2800, 3000, 3200, 3400, 3600, 3800, 4200, 4400, 4600, 4800};

    public static String generate() {
        return "BFEBFBFF000906EB|Intel(R) Core(TM) i3-9100F CPU @ 3.60GHz";
    }

    private String fakeSerial;
    public com.heypixel.heypixelmod.obsoverlay.protocol.HeypixelSession session;
    private static final Map<String, List<String>> EQUIPMENTS = new HashMap<>();
    private static final List<String> RANDOM_MACS = new ArrayList<>();
    public static final Map<String, String> MACS = new LinkedHashMap<>();
    public static final List<String> allIpAddresses = new ArrayList<>();
    private Variable cpu;
    private Variable baseboardInfo;
    private Variable diskStoreInfo;
    private Variable networkInterfaceInfo;
    private Variable neteaseEmails;
    private FakeDiskStore fakeDiskStore;
    private Baseboard baseboard;
    private final Random random = new Random((long)Minecraft.getInstance().getUser().getName().trim().hashCode());
    BooleanValue heypixel = ValueBuilder.create(this, "ShaoYu Display").setDefaultBooleanValue(true).build().getBooleanValue();
    public static final String CHANNEL_CHECK_NAME = "heypixelmod:s2cevent";
    public static Logger logger = LogManager.getLogger("Naven");
    public SimpleChannel channel;
    public static HeypixelSession heypixelSession;
    public static Protocol INSTANCE = new Protocol();
    public static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    public static final String OFFSET3 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";
    private static final String HEX_CHARS = "0123456789ABCDEF";
    private static final String[] CHINA_TELECOM = new String[]{"133", "149", "153", "173", "177", "180", "181", "189", "199"};
    private boolean hasShownMessages = false;

    {
        this.setSuffix(this.heypixel.getCurrentValue() ? "ShaoYu" : "None");
    }

    public Protocol() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerJoined(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!hasShownMessages) {
            try {
                logger.info("Joining server.....");
                heypixelSession = new HeypixelSession();
                ChatUtils.addChatMessage("[ShaoYu]布吉岛协议加载成功");
                ChatUtils.addChatMessage("[ShaoYu]布吉岛正在检查你的客户端数据，类型Info");
                ChatUtils.addChatMessage("[ShaoYu]布吉岛正在检查你的客户端数据，类型BlackClass");
                ChatUtils.addChatMessage("[ShaoYu]布吉岛正在检查你的客户端数据，类型BlackModule");

                hasShownMessages = true;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error on joining server");
                ChatUtils.addChatMessage("处理协议时发生致命错误");
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLeft(ClientPlayerNetworkEvent.LoggingOut event) {
        hasShownMessages = false;
    }

    public static void sendScanClass1(ResourceLocation channelName, long runtime, String uuid1, String uuid2, int maxKlzSize) throws IOException {
        MessageBufferPacker bufferPacker = getDefaultMessagePack(runtime);
        bufferPacker.packString(uuid1);
        bufferPacker.packString(uuid2);
        bufferPacker.packInt(1);
        bufferPacker.packValue(new Variable().setIntegerValue(maxKlzSize));
        bufferPacker.packValue(new Variable().setIntegerValue(0L));
        bufferPacker.packValue(new Variable().setArrayValue(new ArrayList<>()));
        FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
        writeVarInt(friendlyByteBuf, 1);
        writeByteArray(friendlyByteBuf, bufferPacker.toByteArray());
        NetworkUtils.sendPacketNoEvent(new ServerboundCustomPayloadPacket(channelName, friendlyByteBuf));
    }

    private static MessageBufferPacker getDefaultMessagePack(long runtime1) throws IOException {
        MessageBufferPacker bufferPacker = MessagePack.newDefaultBufferPacker();
        bufferPacker.packLong(runtime1);
        return bufferPacker;
    }

    public static void sendSession(ResourceLocation channelName, long runtime, long runtime1, String uuid1, String uuid2, Value gameSession) throws IOException {
        MessageBufferPacker bufferPacker = getDefaultMessagePack(runtime1);
        ChatUtils.addChatMessage("Random UUID: " + uuid1 + "; Player UUID: " + uuid1);
        bufferPacker.packString(uuid1);
        bufferPacker.packString(uuid2);
        bufferPacker.packValue(new Variable().setIntegerValue(runtime));
        bufferPacker.packValue(new Variable().setIntegerValue(0L));
        bufferPacker.packValue(
                new Variable()
                        .setStringValue(
                                "[minecraft, saturn, entityculling, mixinextras, netease_official, fastload, geckolib, waveycapes, ferritecore, embeddium_extra, heypixelmod, cloth_config, forge, embeddium, rubidium, oculus]"
                        )
        );
        bufferPacker.packValue(new Variable().setStringValue("D:\\MCLDownload\\Game\\.minecraft"));
        bufferPacker.packValue(new Variable().setStringValue("D:\\MCLDownload\\ext\\jre-v64-220420\\jdk17"));
        bufferPacker.packValue(heypixelSession.getCpu());
        bufferPacker.packValue(heypixelSession.getBaseboardInfo());
        bufferPacker.packValue(heypixelSession.getNetworkInterfaceInfo());
        bufferPacker.packValue(heypixelSession.getDiskStoreInfo());
        bufferPacker.packValue(heypixelSession.getNeteaseEmails());
        bufferPacker.packValue(gameSession);
        FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
        writeVarInt(friendlyByteBuf, 2);
        writeByteArray(friendlyByteBuf, bufferPacker.toByteArray());
        NetworkUtils.sendPacketNoEvent(new ServerboundCustomPayloadPacket(channelName, friendlyByteBuf));
    }

    public static void writeVarInt(ByteBuf byteBuf, int value) {
        writeVarLong(byteBuf, value & 0xFFFFFFFFL);
    }

    private static void checkNotNull(Object object, String paramName) {
        if (object == null) {
            throw new NullPointerException(paramName + " cannot be null");
        }
    }

    public static void writeByteArray(ByteBuf byteBuf, byte[] bytes) {
        checkNotNull(bytes, "bytes");
        writeVarInt(byteBuf, bytes.length);
        byteBuf.writeBytes(bytes);
    }

    public static void writeVarLong(ByteBuf byteBuf, long value) {
        if ((value & -128L) == 0L) {
            byteBuf.writeByte((byte) value);
        } else if ((value & -16384L) == 0L) {
            int n = (int) (((value & 127L | 128L) << 8) | (value >>> 7));
            byteBuf.writeShort(n);
        } else {
            writeVarLongExtended(byteBuf, value);
        }
    }

    private static void writeVarLongExtended(ByteBuf byteBuf, long value) {
        if ((value & -128L) == 0L) {
            byteBuf.writeByte((byte) value);
        } else if ((value & -16384L) == 0L) {
            int n = (int) (((value & 127L | 128L) << 8) | (value >>> 7));
            byteBuf.writeShort(n);
        } else if ((value & -2097152L) == 0L) {
            int n = (int) (((value & 127L | 128L) << 16) |
                    ((value >>> 7 & 127L | 128L) << 8) |
                    (value >>> 14));
            byteBuf.writeMedium(n);
        } else if ((value & -268435456L) == 0L) {
            int n = (int) (((value & 127L | 128L) << 24) |
                    ((value >>> 7 & 127L | 128L) << 16) |
                    ((value >>> 14 & 127L | 128L) << 8) |
                    (value >>> 21));
            byteBuf.writeInt(n);
        } else if ((value & -34359738368L) == 0L) {
            int n = (int) (((value & 127L | 128L) << 24) |
                    ((value >>> 7 & 127L | 128L) << 16) |
                    ((value >>> 14 & 127L | 128L) << 8) |
                    (value >>> 21 & 127L | 128L));
            byteBuf.writeInt(n);
            byteBuf.writeByte((int) (value >>> 28));
        } else if ((value & -4398046511104L) == 0L) {
            int n = (int) (((value & 127L | 128L) << 24) |
                    ((value >>> 7 & 127L | 128L) << 16) |
                    ((value >>> 14 & 127L | 128L) << 8) |
                    (value >>> 21 & 127L | 128L));
            int n2 = (int) (((value >>> 28 & 127L | 128L) << 8) | (value >>> 35));
            byteBuf.writeInt(n);
            byteBuf.writeShort(n2);
        } else if ((value & -562949953421312L) == 0L) {
            int n = (int) (((value & 127L | 128L) << 24) |
                    ((value >>> 7 & 127L | 128L) << 16) |
                    ((value >>> 14 & 127L | 128L) << 8) |
                    (value >>> 21 & 127L | 128L));
            int n3 = (int) (((value >>> 28 & 127L | 128L) << 16) |
                    ((value >>> 35 & 127L | 128L) << 8) |
                    (value >>> 42));
            byteBuf.writeInt(n);
            byteBuf.writeMedium(n3);
        } else if ((value & -72057594037927936L) == 0L) {
            long l2 = (value & 127L | 128L) << 56 |
                    (value >>> 7 & 127L | 128L) << 48 |
                    (value >>> 14 & 127L | 128L) << 40 |
                    (value >>> 21 & 127L | 128L) << 32 |
                    (value >>> 28 & 127L | 128L) << 24 |
                    (value >>> 35 & 127L | 128L) << 16 |
                    (value >>> 42 & 127L | 128L) << 8 |
                    value >>> 49;
            byteBuf.writeLong(l2);
        } else if ((value & Long.MIN_VALUE) == 0L) {
            long l3 = (value & 127L | 128L) << 56 |
                    (value >>> 7 & 127L | 128L) << 48 |
                    (value >>> 14 & 127L | 128L) << 40 |
                    (value >>> 21 & 127L | 128L) << 32 |
                    (value >>> 28 & 127L | 128L) << 24 |
                    (value >>> 35 & 127L | 128L) << 16 |
                    (value >>> 42 & 127L | 128L) << 8 |
                    (value >>> 49 & 127L | 128L);
            byteBuf.writeLong(l3);
            byteBuf.writeByte((byte) (value >>> 56));
        } else {
            long l4 = (value & 127L | 128L) << 56 |
                    (value >>> 7 & 127L | 128L) << 48 |
                    (value >>> 14 & 127L | 128L) << 40 |
                    (value >>> 21 & 127L | 128L) << 32 |
                    (value >>> 28 & 127L | 128L) << 24 |
                    (value >>> 35 & 127L | 128L) << 16 |
                    (value >>> 42 & 127L | 128L) << 8 |
                    (value >>> 49 & 127L | 128L);
            long l5 = (value >>> 56 & 127L | 128L) << 8 | value >>> 63;
            byteBuf.writeLong(l4);
            byteBuf.writeShort((int) l5);
        }
    }

    public static class HeypixelSession {
        public Value getCpu() {
            return new Variable().setStringValue("CPU_INFO_PLACEHOLDER");
        }

        public Value getBaseboardInfo() {
            return new Variable().setStringValue("BASEBOARD_INFO_PLACEHOLDER");
        }

        public Value getNetworkInterfaceInfo() {
            return new Variable().setStringValue("NETWORK_INFO_PLACEHOLDER");
        }

        public Value getDiskStoreInfo() {
            return new Variable().setStringValue("DISK_INFO_PLACEHOLDER");
        }

        public Value getNeteaseEmails() {
            return new Variable().setStringValue("EMAILS_PLACEHOLDER");
        }
    }

    private Variable getCPUVar() {
        String cpu = YaoMaoFucker.getFakeCpu();
        List<Value> values = new ArrayList<>();
        values.add(ValueFactory.newString(this.generateCPUID(cpu)));
        values.add(ValueFactory.newString(cpu));
        values.add(ValueFactory.newString(YaoMaoFucker.getFakeCpuIdf()));
        return new Variable().setArrayValue(values);
    }

    public Variable getDiskStoreInfoVar() {
        List<Value> values = new ArrayList<>();
        List<Value> valueMap = new ArrayList<>();
        valueMap.add(ValueFactory.newString(this.fakeDiskStore.getSerial()));
        valueMap.add(ValueFactory.newString(this.fakeDiskStore.getName()));
        valueMap.add(ValueFactory.newString(this.fakeDiskStore.getModel()));
        values.add(ValueFactory.newArray(valueMap));
        return new Variable().setArrayValue(values);
    }

    public Variable getBaseboardInfoVar() {
        String[] info = YaoMaoFucker.getBaseboardInfo();
        String manufacturer = info[0];
        String model = info[3];
        String serialNumber = info[1];
        String version = info[2];
        List<Value> valueArray = new ArrayList<>();
        valueArray.add(ValueFactory.newString(manufacturer));
        valueArray.add(ValueFactory.newString(model));
        valueArray.add(ValueFactory.newString(serialNumber));
        valueArray.add(ValueFactory.newString("1.0"));
        valueArray.add(ValueFactory.newString(this.genUUID().toString().toUpperCase()));
        return new Variable().setArrayValue(valueArray);
    }

    public Variable getNetworkInterfaceInfoVar() {
        List<Value> arrayList = new ArrayList<>();

        for (Map.Entry<String, String> entry : FakeMac.MACS.entrySet()) {
            List<Value> hashMap = new ArrayList<>();
            hashMap.add(ValueFactory.newString("wlan" + arrayList.size()));
            hashMap.add(ValueFactory.newString(entry.getKey()));
            hashMap.add(ValueFactory.newString(entry.getValue()));
            hashMap.add(ValueFactory.newArray(new ArrayList<>()));
            List<String> strings = List.of(FakeMac.allIpAddresses.get(this.random.nextInt(FakeMac.allIpAddresses.size())));
            hashMap.add(ValueFactory.newString(strings.toString()));
            arrayList.add(ValueFactory.newArray(hashMap));
        }

        return new Variable().setArrayValue(arrayList);
    }

    public Variable getNeteaseEmailsVar() {
        boolean enable = false;

        for (File var5 : Minecraft.getInstance().gameDirectory.getAbsoluteFile().getParentFile().getParentFile().listFiles()) {
            if (var5.isDirectory() && !var5.getName().equals(".minecraft") && var5.getName().contains("@")) {
                enable = true;
            }
        }

        List<Value> list = new ArrayList<>();
        if (enable) {
            try {
                list.add(ValueFactory.newString(Base64.getEncoder().encodeToString((this.createPhoneNumber() + "@163.com").getBytes())));
            } catch (Exception var6) {
                list.add(ValueFactory.newString("ODk2NDMzMzMzMzNAMTYzLmNvbQ=="));
                JOptionPane.showConfirmDialog(null, var6.getMessage());
            }
        }

        return new Variable().setArrayValue(list);
    }

    public String createPhoneNumber() {
        StringBuilder builder = new StringBuilder();
        String mobilePrefix = null;
        mobilePrefix = CHINA_TELECOM[this.random.nextInt(CHINA_TELECOM.length)];
        builder.append(mobilePrefix);

        for (int i = 0; i < 8; i++) {
            int temp = this.random.nextInt(10);
            builder.append(temp);
        }

        return builder.toString();
    }

    public String generateCPUID(String cpu) {
        StringBuilder cpuid = new StringBuilder();
        if (cpu.contains("Intel")) {
            cpuid.append("BFEBFBFF");
        } else {
            for (int i = 0; i < 8; i++) {
                cpuid.append("0123456789ABCDEF".charAt(this.random.nextInt(16)));
            }
        }

        cpuid.append("000");

        for (int i = 0; i < 5; i++) {
            cpuid.append("0123456789ABCDEF".charAt(this.random.nextInt(16)));
        }

        return cpuid.toString();
    }

    public UUID genUUID() {
        long mostSigBits = this.random.nextLong();
        long leastSigBits = this.random.nextLong();
        return new UUID(mostSigBits, leastSigBits);
    }

    @Override
    public String toString() {
        return "HeypixelSession{\n, cpu="
                + this.cpu
                + "\n, baseboardInfo="
                + this.baseboardInfo
                + "\n, diskStoreInfo="
                + this.diskStoreInfo
                + "\n, networkInterfaceInfo="
                + this.networkInterfaceInfo
                + "\n, neteaseEmails="
                + this.neteaseEmails
                + "}";
    }

    public Variable getCpu() {
        return this.cpu;
    }

    public Variable getBaseboardInfo() {
        return this.baseboardInfo;
    }

    public Variable getDiskStoreInfo() {
        return this.diskStoreInfo;
    }

    public Variable getNetworkInterfaceInfo() {
        return this.networkInterfaceInfo;
    }

    public Variable getNeteaseEmails() {
        return this.neteaseEmails;
    }

    public FakeDiskStore getFakeDiskStore() {
        return this.fakeDiskStore;
    }

    public Baseboard getBaseboard() {
        return this.baseboard;
    }

    public Random getRandom() {
        return this.random;
    }

    public static void refresh(Protocol session) {
        RANDOM_MACS.clear();
        EQUIPMENTS.clear();
        String v = randomMAC(session);
        RANDOM_MACS.add(v);
        EQUIPMENTS.put(
                v,
                List.of(
                        "Intel(R) Wi-Fi 6 AX201 160MHz",
                        "Intel(R) Wireless-AC 9560",
                        "Realtek RTL8822BE Wireless LAN 802.11ac PCI-E NIC",
                        "Realtek PCIe GbE Family Controller",
                        "Intel(R) Ethernet Connection (7) I219-V",
                        "Realtek Gaming 2.5GbE Family Controller"
                )
        );
        String v2 = randomMAC(session);
        RANDOM_MACS.add(v2);
        EQUIPMENTS.put(
                v2,
                List.of(
                        "Intel(R) Ethernet Connection (14) I219-V",
                        "Dell Wireless 1820A 802.11ac",
                        "MediaTek MT7921 Wi-Fi 6 Adapter",
                        "Qualcomm QCA61x4A Wireless Network Adapter",
                        "Killer Wi-Fi 6E AX1675x 160MHz Wireless Network Adapter",
                        "Broadcom 802.11ac Network Adapter",
                        "Realtek PCIe 2.5GbE Family Controller"
                )
        );
        String v3 = randomMAC(session);
        RANDOM_MACS.add(v3);
        EQUIPMENTS.put(
                v3,
                List.of(
                        "Intel(R) Dual Band Wireless-AC 8265",
                        "Realtek RTL8125 2.5GbE Controller",
                        "Killer E3000 2.5 Gigabit Ethernet Controller",
                        "Intel(R) Wi-Fi 6E AX210 160MHz"
                )
        );
        MACS.clear();
        String s = randomMAC(session);
        MACS.put("Realtek PCIe GbE Family Controller", s);

        for (int i = 0; i < session.getRandom().nextInt(2); i++) {
            addMac(session);
        }

        MACS.put("Intel(R) Wi-Fi 6 AX201 160MHz", randomMAC(session));

        for (int i = 0; i < 20; i++) {
            String ipv4 = generateInternalIPv4(session.getRandom());
            allIpAddresses.add(ipv4);
        }

        for (int i = 0; i < 20; i++) {
            String ipv6 = generateInternalIPv6(session.getRandom());
            allIpAddresses.add(ipv6);
        }
    }

    private static void addMac(Protocol session) {
        int i = session.getRandom().nextInt(0, RANDOM_MACS.size() - 1);
        String randomMac = RANDOM_MACS.get(i);
        List<String> strings = EQUIPMENTS.get(randomMac);
        String s = strings.get(session.getRandom().nextInt(0, strings.size() - 1));
        if (MACS.containsKey(s)) {
            addMac(session);
        } else {
            MACS.put(s, randomMac);
        }
    }

    private static void addMac(com.heypixel.heypixelmod.obsoverlay.protocol.HeypixelSession session) {
        if (RANDOM_MACS.isEmpty()) return;

        int i = session.getRandom().nextInt(RANDOM_MACS.size());
        String randomMac = RANDOM_MACS.get(i);
        List<String> strings = EQUIPMENTS.get(randomMac);
        if (strings == null || strings.isEmpty()) return;

        String s = strings.get(session.getRandom().nextInt(strings.size()));
        if (MACS.containsKey(s)) {
            addMac(session);
        } else {
            MACS.put(s, randomMac);
        }
    }

    private static String generateInternalIPv4(Random random) {
        int type = random.nextInt(3);
        byte[] bytes = new byte[4];
        switch (type) {
            case 0:
                bytes[0] = 10;
                random.nextBytes(new byte[]{bytes[1], bytes[2], bytes[3]});
                break;
            case 1:
                bytes[0] = -84;
                bytes[1] = (byte)(16 + random.nextInt(16));
                random.nextBytes(new byte[]{bytes[2], bytes[3]});
                break;
            case 2:
            default:
                bytes[0] = -64;
                bytes[1] = -88;
                bytes[2] = (byte)random.nextInt(256);
                bytes[3] = (byte)(1 + random.nextInt(254));
        }

        try {
            InetAddress address = Inet4Address.getByAddress(bytes);
            return address.getHostAddress();
        } catch (UnknownHostException var4) {
            return "192.168.1.100";
        }
    }

    private static String generateInternalIPv6(Random random) {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        if (random.nextBoolean()) {
            bytes[0] = -3;
        } else {
            bytes[0] = -2;
            bytes[1] = (byte)(bytes[1] & 192 | 128);
        }

        try {
            InetAddress address = Inet6Address.getByAddress(bytes);
            return address.getHostAddress();
        } catch (UnknownHostException var3) {
            return "fd00::1";
        }
    }

    public static String randomMAC(Protocol session) {
        Random rand = session.getRandom();
        byte[] macAddr = new byte[6];
        rand.nextBytes(macAddr);
        macAddr[0] &= -2;
        int vendor = rand.nextInt(5);
        switch (vendor) {
            case 0:
                macAddr[0] = 0;
                macAddr[1] = 31;
                macAddr[2] = 59;
                break;
            case 1:
                macAddr[0] = 0;
                macAddr[1] = -32;
                macAddr[2] = 76;
                break;
            case 2:
                macAddr[0] = 0;
                macAddr[1] = 20;
                macAddr[2] = 34;
                break;
            case 3:
                macAddr[0] = 0;
                macAddr[1] = 26;
                macAddr[2] = -110;
                break;
            case 4:
                macAddr[0] = 0;
                macAddr[1] = 38;
                macAddr[2] = 55;
        }

        StringBuilder sb = new StringBuilder(18);

        for (byte b : macAddr) {
            if (sb.length() > 0) {
                sb.append(":");
            }

            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    public String getSerial() {
        if (this.fakeSerial == null) {
            byte[] randomBytes = new byte[16];
            this.session.getRandom().nextBytes(randomBytes);
            randomBytes[6] = (byte)(randomBytes[6] & 15);
            randomBytes[6] = (byte)(randomBytes[6] | 64);
            randomBytes[8] = (byte)(randomBytes[8] & 63);
            randomBytes[8] = (byte)(randomBytes[8] | 128);
            long msb = 0L;
            long lsb = 0L;

            for (int i = 0; i < 8; i++) {
                msb = msb << 8 | (long)(randomBytes[i] & 255);
            }

            for (int i = 8; i < 16; i++) {
                lsb = lsb << 8 | (long)(randomBytes[i] & 255);
            }

            UUID uuid = new UUID(msb, lsb);
            this.fakeSerial = "{" + uuid.toString() + "}";
        }

        return this.fakeSerial;
    }

    public String getModel() {
        return "Microsoft Storage Space Device (标准磁盘驱动器)";
    }
}