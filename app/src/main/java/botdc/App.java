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
        .directory(".")  // Use relative path
        .ignoreIfMissing() // Add error handling for missing .env
        .load();
    private static final Snowflake USER_ID;
    static {
        String userId = dotenv.get("USER_ID");
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("USER_ID no está configurado en el archivo .env");
        }
        try {
            USER_ID = Snowflake.of(userId);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("USER_ID inválido en el archivo .env: " + userId);
        }
    }

    private static AudioPlayerManager playerManager;
    private static AudioPlayer player;

    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) {
        Validaciones.validateEnvironmentVariables();
        
        DiscordClient client;
        try {
            client = DiscordClient.create(dotenv.get("DISCORD_TOKEN"));
            
            // Inicializar el reproductor de audio de manera segura
            playerManager = new DefaultAudioPlayerManager();
            AudioSourceManagers.registerLocalSource(playerManager);
            playerManager.getConfiguration()
                        .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
            player = playerManager.createPlayer();
            
            // Sanitize file path
            final String safeSoundFilePath = Validaciones.sanitizePath(dotenv.get("SOUND_FILE_PATH"));
            
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

            //Conexion con el gateway de discord y metodo principal para ejecutar eventos
            client.withGateway(gateway -> {
                System.out.println("Bot conectado al Gateway de Discord");

                // Manejar eventos de mensajes
                Eventos.ManejarEventosDeMensajes(gateway);
                

                // Manejar eventos de voz
                Eventos.ManejarEventosDeAudio(gateway, USER_ID, safeSoundFilePath, playerManager, player);

                return Mono.empty();
            })
            .doOnSuccess(ignore -> System.out.println("Conexión al Gateway de Discord establecida correctamente"))
            .doOnError(error -> System.err.println("Error al conectar al Gateway de Discord: " + error.getMessage()))
            .block();
        } catch (Exception e) {
            System.err.println("Error crítico: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (player != null) {
                player.destroy();
            }
            if (playerManager != null) {
                playerManager.shutdown();
            }
        }
    }
}