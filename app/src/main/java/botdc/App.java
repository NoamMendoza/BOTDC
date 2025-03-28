package botdc;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.voice.VoiceConnection;
import io.github.cdimascio.dotenv.Dotenv;
import reactor.core.publisher.Mono;

public class App {
    private static final Dotenv dotenv = Dotenv.configure()
        .directory("C:/Users/Noam/Downloads/BotDC/app") // Especifica la ruta del archivo .env
        .load();
    private static final String TOKEN = dotenv.get("DISCORD_TOKEN");
    static Snowflake USER_ID = Snowflake.of(dotenv.get("USER_ID")); // USER_ID actualizado
    private static final String SOUND_FILE_PATH = dotenv.get("SOUND_FILE_PATH"); // Ruta al archivo de sonido

    private static AudioPlayerManager playerManager;
    private static AudioPlayer player;

    public static void main(String[] args) {
        DiscordClient client = DiscordClient.create(TOKEN);

        // Inicializar el reproductor de audio
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerLocalSource(playerManager);
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        player = playerManager.createPlayer();
        
        // Configurar volumen inicial
        player.setVolume(100);

        // Escuchar eventos de finalización de pista
        player.addListener(new AudioEventAdapter() {
            @Override
            public void onTrackEnd(AudioPlayer player, AudioTrack track, com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason endReason) {
                if (endReason.mayStartNext) {
                    System.out.println("La pista ha terminado. Desconectando al bot del canal de voz...");

                    // Obtener la conexión de voz asociada con la pista
                    VoiceConnection connection = (VoiceConnection) track.getUserData();
                    if (connection != null) {
                        connection.disconnect().block(); // Desconectar al bot del canal de voz
                    }
                }
            }
        });

        client.withGateway(gateway -> {
            System.out.println("Bot conectado al Gateway de Discord");

            // Manejar eventos de mensajes
            Eventos.ManejarEventosDeMensajes(gateway);
            

            // Manejar eventos de voz
            Eventos.ManejarEventosDeAudio(gateway, USER_ID, SOUND_FILE_PATH, playerManager, player);

            return Mono.empty();
        })
        .doOnSuccess(ignore -> System.out.println("Conexión al Gateway de Discord establecida correctamente"))
        .doOnError(error -> System.err.println("Error al conectar al Gateway de Discord: " + error.getMessage()))
        .block();
    }
}