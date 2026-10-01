package cm.gimac.des.gateway.security.session;

import cm.gimac.des.gateway.domain.connexion.CauseEchec;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Réponse application/problem+json portant le code du contrat (champ « code »). */
public final class ReponseProbleme {

    private ReponseProbleme() {}

    /** Corps RFC 9457 : type, title, status, detail, instance, code. */
    public static Map<String, Object> corps(CauseEchec cause, String instance) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("type", "about:blank");
        HttpStatus statut = HttpStatus.resolve(cause.statut());
        corps.put("title", statut == null ? "Error" : statut.getReasonPhrase());
        corps.put("status", cause.statut());
        corps.put("detail", cause.message());
        corps.put("instance", instance);
        corps.put("code", cause.code());
        return corps;
    }

    /** Écrit la réponse d'un filtre (hors contrôleur). */
    public static Mono<Void> ecrire(ServerWebExchange exchange, CauseEchec cause) {
        var reponse = exchange.getResponse();
        reponse.setStatusCode(HttpStatusCode.valueOf(cause.statut()));
        reponse.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        StringBuilder json = new StringBuilder("{");
        boolean premier = true;
        for (Map.Entry<String, Object> e : corps(cause, exchange.getRequest().getPath().value()).entrySet()) {
            if (!premier) {
                json.append(',');
            }
            premier = false;
            json.append('"').append(e.getKey()).append("\":");
            Object v = e.getValue();
            json.append(v instanceof Number ? v.toString() : '"' + echapper(String.valueOf(v)) + '"');
        }
        json.append('}');
        DataBuffer tampon = reponse.bufferFactory().wrap(json.toString().getBytes(StandardCharsets.UTF_8));
        return reponse.writeWith(Mono.just(tampon));
    }

    static String echapper(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        b.append("\\u%04x".formatted((int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.toString();
    }
}
