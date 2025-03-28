package botdc;

import java.io.File;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.VoiceChannel;
import reactor.core.publisher.Mono;

public class Eventos {
    public static void ManejarEventosDeMensajes(GatewayDiscordClient gateway) {
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
    }

    @SuppressWarnings("deprecation")
    public static void ManejarEventosDeAudio(GatewayDiscordClient gateway, Snowflake USER_ID, String SOUND_FILE_PATH, AudioPlayerManager playerManager, AudioPlayer player) {
        gateway.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(event -> {
            VoiceState state = event.getCurrent();
            Member member = state.getMember().block();

            // Verifica si el usuario específico se conectó a un canal de voz
            if (member != null && member.getId().equals(USER_ID)) {
                VoiceChannel channel = state.getChannel().block();
                if (channel != null) {
                    System.out.println("Usuario " + member.getUsername() + " se ha unido al canal de voz: " + channel.getName());

                    // Verificar archivo de audio
                    System.out.println("Cargando archivo de audio desde: " + SOUND_FILE_PATH);
                    File audioFile = new File(SOUND_FILE_PATH);
                    if (!audioFile.exists()) {
                        System.err.println("¡El archivo de audio no existe!");
                        return;
                    }
                    if (!audioFile.canRead()) {
                        System.err.println("¡No se puede leer el archivo de audio!");
                        return;
                    }

                    // Asegurarse de que el reproductor esté limpio
                    player.stopTrack();
                    player.setVolume(100);

                    // Unirse al canal y reproducir
                    channel.join(spec -> 
                        spec.setProvider(new LavaplayerAudioProvider(player))
                    )
                    .flatMap(voiceConnection -> {
                        System.out.println("Bot conectado al canal de voz: " + channel.getName());
                        
                        // Crear instancia de AudioLoadHandler con la conexión de voz
                        AudioLoadHandler handler = new AudioLoadHandler(player, voiceConnection);
                        
                        try {
                            // Cargar y reproducir el sonido usando el handler
                            System.out.println("Intentando cargar el archivo: " + audioFile.getAbsolutePath());
                            playerManager.loadItem(audioFile.getAbsolutePath(), handler);
                        } catch (Exception e) {
                            System.err.println("Error al cargar el archivo: " + e.getMessage());
                        }
                        
                        return Mono.empty();
                    })
                    .doOnError(error -> {
                        System.err.println("Error en la conexión de voz: " + error.getMessage());
                    })
                    .subscribe();
                } else {
                    System.out.println("El usuario " + member.getUsername() + " no está en un canal de voz.");
                }
            }
        });
    }
}
