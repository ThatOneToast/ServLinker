package org.grill.servlinker.client.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AckC2SPacket(String key) implements CustomPayload {
    public static final Id<AckC2SPacket> ID = new Id<>(Identifier.of("runebound", "ack_packet"));

    public static final PacketCodec<RegistryByteBuf, AckC2SPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, AckC2SPacket::key, AckC2SPacket::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
