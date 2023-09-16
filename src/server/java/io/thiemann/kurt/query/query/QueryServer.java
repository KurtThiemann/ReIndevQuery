package io.thiemann.kurt.query.query;

import com.fox2code.foxloader.network.NetworkPlayer;
import io.thiemann.kurt.query.query.packet.*;
import net.minecraft.server.MinecraftServer;
import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class QueryServer {
    private final MinecraftServer server;
    private final DatagramSocket socket;
    private final Thread thread;
    private final Map<String, Integer> challenges = new ConcurrentHashMap<>();
    private long lastChallengeClear = 0;

    public QueryServer(MinecraftServer server, int port, InetAddress laddr) throws SocketException {
        this.server = server;
        this.socket = new DatagramSocket(port, laddr);
        this.thread = new Thread(this::run);
        this.thread.start();

    }

    private void run() {
        while (!this.socket.isClosed()) {
            if (System.currentTimeMillis() - this.lastChallengeClear > 30000) {
                this.challenges.clear();
                this.lastChallengeClear = System.currentTimeMillis();
            }

            try {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                this.socket.receive(packet);
                this.handlePacket(packet);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Handle incoming packet
     * <a href="https://wiki.vg/Query#Client_to_Server_Packet_Format">Packet format</a>
     *
     * @param msg incoming packet
     */
    private void handlePacket(DatagramPacket msg) {
        ServerBoundPacket packet;
        try {
            packet = ServerBoundPacket.fromBuffer(msg.getData(), msg.getLength());
        } catch (QueryProtocolException e) {
            return;
        }

        ClientBoundPacket response;
        if (packet instanceof ServerBoundHandshakePacket) {
            response = this.handshake((ServerBoundHandshakePacket) packet, msg.getSocketAddress());
        } else if (packet instanceof ServerBoundStatPacket) {
            if (((ServerBoundStatPacket) packet).isFull()) {
                response = this.fullStat((ServerBoundStatPacket) packet, msg.getSocketAddress());
            } else {
                response = this.basicStat((ServerBoundStatPacket) packet, msg.getSocketAddress());
            }
        } else {
            return;
        }

        if (response == null) {
            return;
        }

        byte[] payload = response.serialize();
        DatagramPacket responsePacket = new DatagramPacket(payload, payload.length, msg.getSocketAddress());
        try {
            this.socket.send(responsePacket);
        } catch (IOException ignored) {
        }
    }

    private ClientBoundHandshakePacket handshake(ServerBoundHandshakePacket packet, SocketAddress address) {
        int randomToken = ((int) (Math.random() * 1000000)) & 0x0F0F0F0F;
        this.challenges.put(address.toString(), randomToken);

        return new ClientBoundHandshakePacket(packet.getSessionId(), randomToken);
    }

    private ClientBoundBasicStatPacket basicStat(ServerBoundStatPacket packet, SocketAddress address) {
        if (!this.challenges.containsKey(address.toString())) {
            return null;
        }

        if (packet.getChallenge() != this.challenges.get(address.toString())) {
            return null;
        }

        return new ClientBoundBasicStatPacket(
                packet.getSessionId(),
                this.server.getMotd(),
                "SMP",
                "world",
                this.server.configManager.playersOnline(),
                this.server.configManager.getMaxPlayers(),
                this.getMinecraftServerPort(),
                this.getMinecraftServerIp()
        );
    }

    private ClientBoundFullStatPacket fullStat(ServerBoundStatPacket packet, SocketAddress address) {
        if (!this.challenges.containsKey(address.toString())) {
            return null;
        }

        if (packet.getChallenge() != this.challenges.get(address.toString())) {
            System.out.println("Wrong challenge: " + packet.getChallenge() + " != " + this.challenges.get(address.toString()) + " (" + address.toString() + ")");
            return null;
        }

        return new ClientBoundFullStatPacket(
                packet.getSessionId(),
                this.server.getMotd(),
                "SMP",
                "MINECRAFT",
                "Beta 1.7",
                "FoxLoader",
                "world",
                this.server.configManager.playersOnline(),
                this.server.configManager.getMaxPlayers(),
                this.getMinecraftServerPort(),
                this.getMinecraftServerIp(),
                this.server.configManager.playerEntities.stream().map(NetworkPlayer::getPlayerName).toArray(String[]::new)
        );
    }

    private int getMinecraftServerPort() {
        return this.server.propertyManagerObj.getIntProperty("server-port", 25565);
    }

    private String getMinecraftServerIp() {
        String address = this.server.propertyManagerObj.getStringProperty("server-ip", "");
        if (address.length() == 0) {
            address = "127.0.0.1";
        }
        return address;
    }

    public void close() {
        this.socket.close();
        this.thread.interrupt();
    }
}
