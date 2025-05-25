package org.grill.servlinker.client.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record KeyInputToggleC2SPacket(String key) implements CustomPayload {
    public static final Id<KeyInputToggleC2SPacket> ID = new Id<>(Identifier.of("runebound", "key_input_toggle_packet"));

    public static final PacketCodec<RegistryByteBuf, KeyInputToggleC2SPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, KeyInputToggleC2SPacket::key, KeyInputToggleC2SPacket::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
