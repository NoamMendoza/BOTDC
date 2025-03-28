package botdc;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.voice.VoiceConnection;

public class AudioLoadHandler implements AudioLoadResultHandler {
    private final AudioPlayer player;
    private final VoiceConnection voiceConnection;

    public AudioLoadHandler(AudioPlayer player, VoiceConnection voiceConnection) {
        this.player = player;
        this.voiceConnection = voiceConnection;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        try {
            System.out.println("Iniciando reproducción de: " + track.getInfo().title);
            
            // Asegurarnos de que el track tenga la conexión de voz
            track.setUserData(voiceConnection);
                
            // Detener cualquier reproducción actual
            player.stopTrack();
                
            // Configurar volumen y reproducir
            player.setVolume(100);
            player.playTrack(track.makeClone());
                   
            System.out.println("Reproducción iniciada correctamente");
        } catch (Exception e) {
            System.err.println("Error al reproducir la pista: " + e.getMessage());
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        System.out.println("Lista de reproducción cargada: " + playlist.getName());
        if (!playlist.getTracks().isEmpty()) {
            trackLoaded(playlist.getTracks().get(0));
        }
    }

    @Override
    public void noMatches() {
        System.out.println("No se encontró el archivo de audio en la ruta especificada");
        System.out.println("Por favor, verifique que la ruta del archivo sea correcta y que el archivo exista");
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        System.out.println("Error al cargar el archivo de audio: " + exception.getMessage());
    }
}
