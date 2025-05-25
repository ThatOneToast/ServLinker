package org.grill.servlinker.client.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncS2CPacket(String key) implements CustomPayload {
    public static final Id<SyncS2CPacket> ID = new Id<>(Identifier.of("runebound", "sync_packet"));

    public static final PacketCodec<RegistryByteBuf, SyncS2CPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, SyncS2CPacket::key, SyncS2CPacket::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

