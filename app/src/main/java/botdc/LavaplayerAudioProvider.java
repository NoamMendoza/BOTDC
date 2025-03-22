package botdc; // Asegúrate de que esté en el mismo paquete que App.java

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import discord4j.voice.AudioProvider;
import java.nio.ByteBuffer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

public class LavaplayerAudioProvider extends AudioProvider {
    private final AudioPlayer player;

    public LavaplayerAudioProvider(AudioPlayer player) {
        super(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));
        this.player = player;
    }

    @Override
    public boolean provide() {
        AudioFrame frame = player.provide();
        if (frame != null) {
            getBuffer().put(frame.getData());
            getBuffer().flip();
            return true;
        }
        return false;
    }
}