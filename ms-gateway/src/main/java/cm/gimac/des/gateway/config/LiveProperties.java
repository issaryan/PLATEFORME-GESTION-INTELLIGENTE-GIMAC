package cm.gimac.des.gateway.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propriétés du canal poussé des couches de présentation (gimac.live). */
@ConfigurationProperties(prefix = "gimac.live")
public class LiveProperties {

    /** Consommation Kafka active ; inactif, le canal reste servi mais ne reçoit aucun événement. */
    private boolean actif = true;

    /** Courtiers Kafka (spring.kafka.bootstrap-servers ou courtiers du binder par défaut). */
    private String bootstrapServers = "localhost:9092";

    /** Sujets Kafka consommés ; le sujet fonctionnel est leur premier segment. */
    private List<String> sujets = new ArrayList<>(
        List.of(
            "homologation.referential",
            "homologation.liaison",
            "homologation.operation",
            "homologation.campaign",
            "homologation.coverage",
            "homologation.notification",
            "certification.circuit",
            "certification.quitus",
            "certification.dossier",
            "certification.attestation",
            "certification.notification",
            "collaboration.message"
        )
    );

    /** Propriétés supplémentaires du consommateur (sécurité du courtier…). */
    private Map<String, String> kafka = new LinkedHashMap<>();

    /** Événements gardés pour la reprise (« depuis », Last-Event-ID). */
    private int tailleTampon = 500;

    /** Âge maximal d'un événement du tampon de reprise. */
    private Duration dureeTampon = Duration.ofMinutes(5);

    /** Intervalle du commentaire de maintien de la connexion. */
    private Duration battement = Duration.ofSeconds(25);

    /** Durée maximale d'une connexion : le navigateur se reconnecte et ses droits sont réévalués. */
    private Duration dureeMaxConnexion = Duration.ofMinutes(30);

    /** Événements en attente par connexion avant abandon des plus anciens. */
    private int tamponConnexion = 256;

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public List<String> getSujets() {
        return sujets;
    }

    public void setSujets(List<String> sujets) {
        this.sujets = new ArrayList<>(sujets);
    }

    public Map<String, String> getKafka() {
        return kafka;
    }

    public void setKafka(Map<String, String> kafka) {
        this.kafka = new LinkedHashMap<>(kafka);
    }

    public int getTailleTampon() {
        return tailleTampon;
    }

    public void setTailleTampon(int tailleTampon) {
        this.tailleTampon = tailleTampon;
    }

    public Duration getDureeTampon() {
        return dureeTampon;
    }

    public void setDureeTampon(Duration dureeTampon) {
        this.dureeTampon = dureeTampon;
    }

    public Duration getBattement() {
        return battement;
    }

    public void setBattement(Duration battement) {
        this.battement = battement;
    }

    public Duration getDureeMaxConnexion() {
        return dureeMaxConnexion;
    }

    public void setDureeMaxConnexion(Duration dureeMaxConnexion) {
        this.dureeMaxConnexion = dureeMaxConnexion;
    }

    public int getTamponConnexion() {
        return tamponConnexion;
    }

    public void setTamponConnexion(int tamponConnexion) {
        this.tamponConnexion = tamponConnexion;
    }
}
