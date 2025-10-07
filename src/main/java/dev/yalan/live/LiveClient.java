package dev.yalan.live;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventManager;
import dev.yalan.live.events.EventLiveConnectionStatus;
import dev.yalan.live.netty.LiveProto;
import dev.yalan.live.netty.codec.FrameDecoder;
import dev.yalan.live.netty.codec.FrameEncoder;
import dev.yalan.live.netty.codec.crypto.RSADecoder;
import dev.yalan.live.netty.codec.crypto.RSAEncoder;
import dev.yalan.live.netty.handler.LiveHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.minecraft.client.Minecraft;
import oshi.SystemInfo;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class LiveClient {
    public static LiveClient INSTANCE;

    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("Live-Worker"));
    private final LiveReconnectionThread reconnectionThread = new LiveReconnectionThread();
    private final EventManager eventManager = new EventManager();
    private final AtomicBoolean isConnecting = new AtomicBoolean();
    private final LiveComponent liveComponent = new LiveComponent(this);
    public final AtomicReference<String> autoUsername2 = new AtomicReference<>();
    private final HashMap<UUID, LiveUser> liveUserMap = new HashMap<>();
    private final RSAPrivateKey rsaPrivateKey;
    private final RSAPublicKey rsaPublicKey;
    private final String hardwareId;

    public int serversideProtocolVersion = LiveProto.PROTOCOL_VERSION;
    public String autoUsername;
    public String autoPassword;
    public LiveUser liveUser;

    private Channel channel;

    public LiveClient() {
        try {
            rsaPrivateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC+JD/BPJiV6P9jOByok9N0wwRd/lPUkOrmJiqb9gxtpZBHg7tFT9kZhIC0txNAyRXBAogY72bJF9w/neh1khnKYPawg3WA/cqVwG44Ohx3a23j246ilqcUGZEbPxWI9XISnf5KEZj341e3UqCEE1OOQTxTz6EK1dSHpdFgoBgVFU2HowPrzQ8ZKuEWRWeCP1r6ntYK2WHjWGQEm9Vwejsz06AN6zchJUjvKtBqv0mGhMdMxQa8n2c+tF44o6nr4o0qhQFwzouCVxLbKKCW4+B1WhMrEt6m2pI7RHkFrnowuShMRXk4SUhsfqV9DiE2VunSPJDa0LQF+ZDvK1RzDBABAgMBAAECggEAGd2OK11wt6qM8fSETYMNLQvfEGGUX3e2mfKk3Dd3Oa6KjHfv5ei2n0Ew+E8T7ZMNLwbcTba6FEP3Kv5HokAVhbwgAs4Mkiy0E3VXE7YFVlIaY6M5PtvgmZw1au/vdYScwnSUpC3K/Hmu3oTSdMpjlO1B5XglWkx7+gHwFBq4e/GAPvYiaZzUN6r2WLWfYOjS1swYtW2wAkerdH5L7osTPFGcysg5FQf45ak3kCMOCKcUyPVgGAp63pvpjWs+UwbSnTTi2SKdk1hsENGz8y5H5mIRoMNZvRBv8PlMR+KXSMzqOrupMdZNf8vVCUFq+o7C8zKO6/EBJrdSqVEa3O4EwwKBgQDtAC2j4Xfpk/fXyi3h81+7WjIUDTVM/sr5BDnrUPg/G7SXGoG27BsDxdGDe5z9qYDX+6925gj0qNWwmLhMWK5teidMyaMgkLb1WJZW9hxouNPiW0f8W0dE6XY/JEetOVCEKugjnK717+SG2xgRCDzUcwiaTaZjaCbpUBRf41bMVwKBgQDNYmjtDZ0RhTQqVXVsIURVrBXD4qC20+qcZD8UmiPlfmKIDAQHwR2baIdDH13/7gpF6SoXd4YxUDy3L9cyHJnnvdGt/ZCZVZBM8zaN4x+j0uJztpEg1ZqYc03wVfguLgYiEhAFYy63j6Kqzsgxk5L+Nt4Z+ykgF1iRARik89dPZwKBgQCyfTjiPHM/wcWdidHGYrFNe19NxKjIxPd+VRV9yKw9cxMt3bOlXOn3jGr++ADC9X5oq095X0ONZv4QkuPx87PiFWY1qYImi3aPDlmjQpUgtVo2FoL/ZoslNiJs6Vjl981QzLOp5l3KMRJOEgFtCmQtqDjpZOOT6COuATZnBOMg3wKBgFkwnzgybT8qhVjM+80VNUOvE1SZmglLRdrcfbhIp8YFeGx0K1vitSTD49l2Sa/Wg4eQlcLTGOdZXMSAdgdA/GTyvZe9QYoU6jFAfTRoqVjPP5/YbHXBzPzfNb1k1/3V5rvs9CAizirQqWdbnPxKhc73rMPDmjxhZwixXS5k5d+JAoGBAKZLVzkRHHEJppmrwqEGLFkfNp/PyWmSWnkceax0N9P0HaXns2P8pQKV3TOAb6VPguhkrfn4M7giw662Y1Nx8q92yVAno6ZsZPO1y5twlSoRAK1iFsG9sAhwI6oByJLUs9AsTdASqt8ngSbIdXCGECEhf7z3wJw/JkwW/uKSMwL+")));
            rsaPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2KSkhsDq8KwyNlquFcEFA07wR/RwP+uDL/N7eCjl2iXj4YQCM0t5HAMLLhCLtWYfbvyq6QIriXsgRld9Qig7H8EuQP7yrEjFoECytQX8dU6iSzJeFHUrbQDRSljDx/xk88ywkU4AmsYsWiabvCsdIBT1sly8HZzSmV2EenlWFRRVQCBTNbcXQrb/lE5cYmlULOjeJ/lyTpv6b+rjjfb7yO3iSLptfEFmWGXuMIiUVoQRZF8wE7f2V56kPVwV3pjZUKgcQUHxEVpNPuCVuqBIWAfgDjGkLoJMk08kbbcAOt2E6O8bGykD25eA/9+yF0Zqv8dvcOnXww6CnWrjrB0h/QIDAQAB")));
            hardwareId = generateHardwareId();
        } catch (Exception e) {
            throw new RuntimeException("Can't init LiveClient", e);
        }
    }

    public void connect() {
        if (isOpen() || isConnecting.get()) {
            return;
        }

        isConnecting.set(true);

        final Bootstrap bootstrap = new Bootstrap()
                .channel(NioSocketChannel.class)
                .group(workerGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast("frame_decoder", new FrameDecoder())
                                .addLast("rsa_decoder", new RSADecoder(rsaPrivateKey))
                                .addLast("frame_encoder", new FrameEncoder())
                                .addLast("rsa_encoder", new RSAEncoder(rsaPublicKey))
                                .addLast("live_handler", new LiveHandler(LiveClient.this));
                    }
                });

        bootstrap.connect("live.unitednetwork.cc", 7851).addListener((ChannelFutureListener) future -> {
            isConnecting.set(false);

            if (future.isSuccess()) {
                LiveProto.sendPacket(future.channel(), LiveProto.createHandshake());
            }

            Minecraft.getInstance().execute(() -> {
                if (future.isSuccess()) {
                    channel = future.channel();
                }

                eventManager.call(new EventLiveConnectionStatus(future.isSuccess(), future.cause()));
            });
        });
    }

    public void sendPacket(LiveProto.LivePacket packet) {
        if (isActive()) {
            LiveProto.sendPacket(channel, packet);
        }
    }

    public void shutdown() {
        stopReconnectionThread();

        if (isOpen()) {
            channel.close();
        }

        workerGroup.shutdownGracefully();
    }

    public void startReconnectionThread() {
        reconnectionThread.start();
    }

    public void stopReconnectionThread() {
        if (reconnectionThread.isAlive()) {
            reconnectionThread.interrupt();
        }
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public boolean isConnecting() {
        return isConnecting.get();
    }

    public boolean isOpen() {
        return channel != null && channel.isOpen();
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    public LiveComponent getLiveComponent() {
        return liveComponent;
    }

    public HashMap<UUID, LiveUser> getLiveUserMap() {
        return liveUserMap;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    private static String generateHardwareId() throws Exception {
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        final SystemInfo systemInfo = new SystemInfo();
        final String info =
                systemInfo.getHardware().getProcessor().getProcessorIdentifier().toString()
                + systemInfo.getHardware().getComputerSystem().getBaseboard().getSerialNumber()
                + systemInfo.getHardware().getComputerSystem().getSerialNumber();
        final byte[] digest = messageDigest.digest(info.getBytes(StandardCharsets.UTF_8));
        final StringBuilder digestSB = new StringBuilder();

        for (byte b : digest) {
            final String hexString = Integer.toHexString(b & 0xFF);

            if (hexString.length() == 1) {
                digestSB.append('0').append(hexString);
            } else {
                digestSB.append(hexString);
            }
        }

        return digestSB.toString();
    }
}
