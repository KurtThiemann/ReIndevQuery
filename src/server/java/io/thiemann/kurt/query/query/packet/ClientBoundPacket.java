package io.thiemann.kurt.query.query.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class ClientBoundPacket {
    private final PacketType type;

    private final int sessionId;

    public ClientBoundPacket(PacketType type, int sessionId) {
        this.type = type;
        this.sessionId = sessionId;
    }

    public PacketType getType() {
        return type;
    }

    public int getSessionId() {
        return sessionId;
    }

    protected abstract byte[] serializePayload();

    public byte[] serialize() {
        byte[] payload = serializePayload();
        ByteBuffer buffer = ByteBuffer.allocate(5 + payload.length);
        buffer.order(ByteOrder.BIG_ENDIAN);

        buffer.put(this.getType().getId());
        buffer.putInt(this.getSessionId());

        buffer.put(payload);

        return buffer.array();
    }
}
