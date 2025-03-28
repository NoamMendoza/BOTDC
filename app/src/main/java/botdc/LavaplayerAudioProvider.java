package botdc; // Asegúrate de que esté en el mismo paquete que App.java

import java.nio.ByteBuffer;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import discord4j.voice.AudioProvider;

public class LavaplayerAudioProvider extends AudioProvider {
    private final AudioPlayer player;
    private AudioFrame lastFrame;

    public LavaplayerAudioProvider(AudioPlayer player) {
        super(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));
        this.player = player;
    }

    @Override
    public boolean provide() {
        try {
            lastFrame = player.provide();
            if (lastFrame == null) {
                return false;
            }

            getBuffer().clear();
            getBuffer().put(lastFrame.getData());
            getBuffer().flip();
            return true;
        } catch (Exception e) {
            System.err.println("Error providing audio data: " + e.getMessage());
            return false;
        }
    }
}