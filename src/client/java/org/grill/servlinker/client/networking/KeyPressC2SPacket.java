package org.grill.servlinker.client.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record KeyPressC2SPacket(String key) implements CustomPayload {
    public static final Id<KeyPressC2SPacket> ID = new Id<>(Identifier.of("runebound", "keypress_packet"));

    public static final PacketCodec<RegistryByteBuf, KeyPressC2SPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, KeyPressC2SPacket::key, KeyPressC2SPacket::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
