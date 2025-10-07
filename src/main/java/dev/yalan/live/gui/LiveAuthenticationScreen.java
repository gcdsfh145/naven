package dev.yalan.live.gui;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.files.FileManager;
import dev.yalan.live.LiveClient;
import dev.yalan.live.events.*;
import dev.yalan.live.netty.LiveProto;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class LiveAuthenticationScreen extends Screen {
    private static final Logger logger = LogManager.getLogger("LiveAuthenticationScreen");
    private static final File accountDataFile = new File(FileManager.clientFolder, "LiveAccount.dat");
    private static String savedUsername = "";
    private static String savedPassword = "";
    private static boolean firstTime = true;

    static {
        try {
            loadAccountData();
        } catch (Exception e) {
            logger.error("Can't load live account data", e);
        }
    }

    private EditBox username;
    private EditBox password;
    private Button loginButton;

    private String status = "";

    public LiveAuthenticationScreen() {
        super(Component.literal("Live Authentication"));
    }

    @EventTarget
    public void onLiveChannelActive(EventLiveChannelActive e) {
        loginButton.active = true;
        username.active = true;
        password.active = true;
    }

    @EventTarget
    public void onLiveChannelInactive(EventLiveChannelInactive e) {
        loginButton.active = false;
        username.active = true;
        password.active = true;
    }

    @EventTarget
    public void onLiveConnectionStatus(EventLiveConnectionStatus e) {
        username.active = true;
        password.active = true;

        if (e.isSuccess()) {
            loginButton.active = true;
        } else {
            loginButton.active = false;

            if (e.getCause() != null) {
                logger.error("Can't connect to LiveServer", e.getCause());
                status = "无法连接到服务器: " + e.getCause().toString();
            } else {
                status = "无法连接到服务器: 未知错误";
            }
        }
    }

    @EventTarget
    public void onLiveGenericMessage(EventLiveGenericMessage e) {
        if (e.getChannel().equals("Disconnect")) {
            status = e.getMessage();
        }
    }

    @EventTarget
    public void onLiveAuthenticationResult(EventLiveAuthenticationResult e) {
        loginButton.active = true;

        if (!e.isSuccess()) {
            status = e.getMessage();
            username.active = true;
            password.active = true;
            return;
        }

        try {
            saveAccountData();
        } catch (Exception ex) {
            logger.error("Can't save account data", ex);
        }

        LiveClient.INSTANCE.autoUsername = username.getValue();
        LiveClient.INSTANCE.autoPassword = password.getValue();
        LiveClient.INSTANCE.startReconnectionThread();
        Naven.getInstance().getEventManager().register(LiveClient.INSTANCE.getLiveComponent());

        minecraft.setScreen(new TitleScreen(true));
    }

    @Override
    public void render(GuiGraphics graphics, int p_281550_, int p_282878_, float p_282465_) {
        this.renderBackground(graphics);
        super.render(graphics, p_281550_, p_282878_, p_282465_);

        final int hw = width / 2;
        final int hh = height / 2;

        graphics.drawCenteredString(minecraft.font, "ShaoYu Authentication", hw, hh - 80, -1);
        graphics.drawCenteredString(minecraft.font, status, hw, hh - 65, -1);
        graphics.drawString(minecraft.font, "LiveServer: " + getLiveConnectionStatus(), 2, height - minecraft.font.lineHeight, -1);
        graphics.drawString(minecraft.font, "用户名: ", hw - 82, hh - 45, -1);
        graphics.drawString(minecraft.font, "密码: ", hw - 82, hh - 10, -1);
    }

    private String getLiveConnectionStatus() {
        if (LiveClient.INSTANCE.isActive()) {
            return ChatFormatting.GREEN + "已连接";
        }

        if (LiveClient.INSTANCE.isConnecting()) {
            return ChatFormatting.YELLOW + "连接中...";
        }

        return ChatFormatting.RED + "无连接";
    }

    @Override
    public void tick() {
        username.tick();
        password.tick();

        if (LiveProto.PROTOCOL_VERSION != LiveClient.INSTANCE.serversideProtocolVersion) {
            status = ChatFormatting.RED + "版本已过期!";
            loginButton.active = false;

            if (LiveClient.INSTANCE.isActive()) {
                LiveClient.INSTANCE.shutdown();
            }
        }
    }

    @Override
    protected void init() {
        final int hw = width / 2;
        final int hh = height / 2;

        username = new EditBox(font, hw - 83, hh - 35, 166, 20, Component.literal("Username"));
        username.setMaxLength(32);
        username.setValue(savedUsername);
        password = new EditBox(font, hw - 83, hh, 166, 20, Component.literal("Username"));
        password.setMaxLength(64);
        password.setValue(savedPassword);

        addRenderableWidget(username);
        addRenderableWidget(password);

        loginButton = addRenderableWidget(Button.builder(Component.literal("登录"), (button) -> {
            if (LiveClient.INSTANCE.isActive()) {
                loginButton.active = false;
                username.active = false;
                password.active = false;
                LiveClient.INSTANCE.sendPacket(LiveProto.createAuthentication(username.getValue(), password.getValue(), LiveClient.INSTANCE.getHardwareId()));
            }
        }).bounds(hw - 83, hh + 25, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("注册"), (button) -> {
            Util.getPlatform().openUri("https://www.unitednetwork.cc/Shaoyu/html?name=RegisterWithEmail&hardwareId=" + URLEncoder.encode(LiveClient.INSTANCE.getHardwareId(), StandardCharsets.UTF_8));
        }).bounds(hw + 3, hh + 25, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("重连"), (button) -> {
            LiveClient.INSTANCE.connect();
        }).bounds(hw - 83, hh + 50, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("退出"), (button) -> {
            minecraft.stop();
        }).bounds(hw + 3, hh + 50, 80, 20).build());
    }

    @Override
    public void added() {
        if (firstTime) {
            LiveClient.INSTANCE = new LiveClient();
        }

        LiveClient.INSTANCE.getEventManager().register(this);

        if (firstTime) {
            firstTime = false;

            LiveClient.INSTANCE.connect();
        }
    }

    @Override
    public void removed() {
        LiveClient.INSTANCE.getEventManager().unregister(this);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private static void loadAccountData() throws Exception {
        if (!accountDataFile.exists()) {
            return;
        }

        final byte[] data = FileUtils.readFileToByteArray(accountDataFile);
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        final SecretKey key = new SecretKeySpec(Base64.getDecoder().decode("uoJlt4v2dBF4Q9IHqK8/kg=="), "AES");
        final byte[] iv = new byte[12];

        System.arraycopy(data, 0, iv, 0, 12);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        cipher.updateAAD(accountDataFile.getAbsolutePath().getBytes(StandardCharsets.UTF_8));

        final byte[] out = cipher.doFinal(data, iv.length, data.length - iv.length);
        final String[] split = new String(out, StandardCharsets.UTF_8).split(System.lineSeparator());

        savedUsername = split[0];
        savedPassword = split[1];
    }

    private void saveAccountData() throws Exception {
        final byte[] data = (username.getValue() + System.lineSeparator() + password.getValue()).getBytes(StandardCharsets.UTF_8);
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        final SecretKey key = new SecretKeySpec(Base64.getDecoder().decode("uoJlt4v2dBF4Q9IHqK8/kg=="), "AES");
        final SecureRandom secureRandom = new SecureRandom();
        final byte[] iv = new byte[12];

        secureRandom.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        cipher.updateAAD(accountDataFile.getAbsolutePath().getBytes(StandardCharsets.UTF_8));

        final byte[] out = new byte[12 + cipher.getOutputSize(data.length)];
        System.arraycopy(iv, 0, out, 0, iv.length);
        cipher.doFinal(data, 0, data.length, out, 12);

        FileUtils.writeByteArrayToFile(accountDataFile, out);
    }
}
