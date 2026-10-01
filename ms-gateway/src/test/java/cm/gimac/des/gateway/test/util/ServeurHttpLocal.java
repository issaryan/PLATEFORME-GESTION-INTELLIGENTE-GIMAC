package cm.gimac.des.gateway.test.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * Serveur HTTP local (com.sun.net.httpserver) qui simule un service de la plateforme,
 * Prometheus, Alertmanager ou Keycloak : réponses programmées par chemin, délai éventuel,
 * requêtes reçues mémorisées.
 */
public final class ServeurHttpLocal implements AutoCloseable {

    /** Requête reçue. */
    public record Recue(String methode, String chemin, String requete, String autorisation, String corps) {}

    private record Reponse(int statut, String typeContenu, String corps, long delaiMs, Map<String, String> entetes) {
        Reponse(int statut, String typeContenu, String corps, long delaiMs) {
            this(statut, typeContenu, corps, delaiMs, Map.of());
        }
    }

    private final HttpServer serveur;
    private final Map<String, Reponse> reponses = new ConcurrentHashMap<>();
    private final List<Recue> recues = new CopyOnWriteArrayList<>();

    public ServeurHttpLocal() throws IOException {
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.setExecutor(Executors.newCachedThreadPool());
        serveur.createContext("/", this::traiter);
        serveur.start();
    }

    public String url() {
        return "http://127.0.0.1:" + serveur.getAddress().getPort();
    }

    public ServeurHttpLocal json(String chemin, int statut, String corps) {
        return repondre(chemin, statut, "application/json", corps, 0);
    }

    public ServeurHttpLocal texte(String chemin, String corps) {
        return repondre(chemin, 200, "text/plain;version=0.0.4;charset=utf-8", corps, 0);
    }

    public ServeurHttpLocal repondre(String chemin, int statut, String typeContenu, String corps, long delaiMs) {
        reponses.put(chemin, new Reponse(statut, typeContenu, corps, delaiMs));
        return this;
    }

    /** Réponse portant des en-têtes supplémentaires (signal d'un service à la passerelle). */
    public ServeurHttpLocal repondre(String chemin, int statut, String typeContenu, String corps, Map<String, String> entetes) {
        reponses.put(chemin, new Reponse(statut, typeContenu, corps, 0, Map.copyOf(entetes)));
        return this;
    }

    public List<Recue> recues() {
        return recues;
    }

    private void traiter(HttpExchange echange) throws IOException {
        String chemin = echange.getRequestURI().getPath();
        String corpsRecu = new String(echange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        recues.add(
            new Recue(
                echange.getRequestMethod(),
                chemin,
                echange.getRequestURI().getRawQuery(),
                echange.getRequestHeaders().getFirst("Authorization"),
                corpsRecu
            )
        );
        Reponse r = reponses.getOrDefault(chemin, new Reponse(404, "application/json", "{}", 0));
        if (r.delaiMs() > 0) {
            try {
                Thread.sleep(r.delaiMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] corps = r.corps().getBytes(StandardCharsets.UTF_8);
        echange.getResponseHeaders().add("Content-Type", r.typeContenu());
        r.entetes().forEach((k, v) -> echange.getResponseHeaders().add(k, v));
        try {
            echange.sendResponseHeaders(r.statut(), corps.length == 0 ? -1 : corps.length);
            if (corps.length > 0) {
                echange.getResponseBody().write(corps);
            }
        } catch (IOException e) {
            // Client parti (délai dépassé côté passerelle) : sans conséquence.
        } finally {
            echange.close();
        }
    }

    @Override
    public void close() {
        serveur.stop(0);
    }
}
