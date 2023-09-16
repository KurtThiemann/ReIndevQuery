package io.thiemann.kurt.query.query.packet;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class ClientBoundBasicStatPacket extends ClientBoundPacket {

    private final String motd;
    private final String gameType;
    private final String map;
    private final int playersOnline;
    private final int maxPlayers;
    private final int port;
    private final String host;

    public ClientBoundBasicStatPacket(int sessionId, String motd, String gameType, String map, int playersOnline, int maxPlayers, int port, String host) {
        super(PacketType.STAT, sessionId);
        this.motd = motd;
        this.gameType = gameType;
        this.map = map;
        this.playersOnline = playersOnline;
        this.maxPlayers = maxPlayers;
        this.port = port;
        this.host = host;
    }

    @Override
    protected byte[] serializePayload() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put((this.motd + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.put((this.gameType + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.put((this.map + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.put((this.playersOnline + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.put((this.maxPlayers + "\0").getBytes(StandardCharsets.UTF_8));
        buffer.putShort((short) this.port);
        buffer.put((this.host + "\0").getBytes(StandardCharsets.UTF_8));

        //only return written length as byte array
        byte[] response = new byte[buffer.position()];
        ((Buffer) buffer).rewind();
        buffer.get(response);
        return response;
    }
}
