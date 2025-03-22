package botdc;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.common.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.ByteBuffer;

public class App {
    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final String TOKEN = dotenv.get("DISCORD_TOKEN");
    private static Snowflake USER_ID = Snowflake.of(dotenv.get("USER_ID")); // USER_ID actualizado
    private static final String SOUND_FILE_PATH = "SOUND_FILE_PATH"; // Ruta al archivo de sonido

    private static AudioPlayerManager playerManager;
    private static AudioPlayer player;

    public static void main(String[] args) {
        DiscordClient client = DiscordClient.create(TOKEN);

        // Inicializar el reproductor de audio
        playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new LocalAudioSourceManager());
        player = playerManager.createPlayer();

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
            gateway.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
                Message message = event.getMessage();
                User autor = message.getAuthor().orElseThrow(() -> new RuntimeException("El autor del mensaje no está presente."));

                // Ignorar mensajes del propio bot
                if (autor.isBot()) {
                    return;
                } else {
                    System.out.println("Id del autor: " + autor.getId().asString());
                    System.out.println("Autor: " + autor.getUsername());
                }

                // Respuesta específica para un usuario
                if (autor.getUsername().equalsIgnoreCase("_tipy")) {
                    message.getChannel()
                        .flatMap(channel -> channel.createMessage("Calla puto negro de mierda"))
                        .subscribe();
                }

                // Respuesta al comando !ping
                if (message.getContent().equalsIgnoreCase("!ping")) {
                    message.getChannel()
                        .flatMap(channel -> channel.createMessage("Pong!"))
                        .subscribe();
                }
            });

            // Manejar eventos de voz
            gateway.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(event -> {
                VoiceState state = event.getCurrent();
                Member member = state.getMember().block();

                // Verifica si el usuario específico se conectó a un canal de voz
                if (member != null && member.getId().equals(USER_ID)) {
                    VoiceChannel channel = state.getChannel().block();
                    if (channel != null) {
                        System.out.println("Usuario " + member.getUsername() + " se ha unido al canal de voz: " + channel.getName());

                        // Conecta el bot al mismo canal de voz
                        channel.join(spec -> spec.setProvider(new LavaplayerAudioProvider(player)))
                            .then(Mono.fromRunnable(() -> {
                                System.out.println("Bot conectado al canal de voz: " + channel.getName());

                                // Carga y reproduce el sonido
                                playerManager.loadItem(SOUND_FILE_PATH, new AudioLoadResultHandler() {
                                    @Override
                                    public void trackLoaded(AudioTrack track) {
                                        System.out.println("Reproduciendo archivo de audio: " + track.getInfo().title);

                                        // Guardar la conexión de voz en la pista
                                        track.setUserData(channel.getVoiceConnection().block());
                                        player.playTrack(track); // Reproduce el track cargado
                                    }

                                    @Override
                                    public void playlistLoaded(AudioPlaylist playlist) {
                                        System.out.println("Lista de reproducción cargada: " + playlist.getName());
                                    }

                                    @Override
                                    public void noMatches() {
                                        System.out.println("No se encontró el archivo de audio.");
                                    }

                                    @Override
                                    public void loadFailed(FriendlyException exception) {
                                        System.out.println("Error al cargar el archivo de audio: " + exception.getMessage());
                                    }
                                });
                            }))
                            .subscribe();
                    } else {
                        System.out.println("El usuario " + member.getUsername() + " no está en un canal de voz.");
                    }
                } else {
                    System.out.println("El usuario no coincide o no se ha unido a un canal de voz.");
                }
            });

            return Mono.empty();
        })
        .doOnSuccess(ignore -> System.out.println("Conexión al Gateway de Discord establecida correctamente"))
        .doOnError(error -> System.err.println("Error al conectar al Gateway de Discord: " + error.getMessage()))
        .block();
    }
}