package cm.gimac.des.gateway.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Sessions de la passerelle (gimac.session, contrat D-65 §6). */
@ConfigurationProperties(prefix = "gimac.session")
public class SessionProperties {

    /** Inactivité maximale : au-delà, la session est fermée (30 min au plus). */
    private Duration inactiviteMax = Duration.ofMinutes(30);

    /** Durée absolue maximale d'une session, quelle que soit l'activité. */
    private Duration dureeMax = Duration.ofHours(10);

    /** Période du balayage qui ferme les sessions échues sans attendre une requête. */
    private Duration balayage = Duration.ofMinutes(1);

    /** Nom du témoin de session (server.reactive.session.cookie.name, SESSION par défaut). */
    private String temoin = "SESSION";

    public Duration getInactiviteMax() {
        return inactiviteMax;
    }

    public void setInactiviteMax(Duration inactiviteMax) {
        this.inactiviteMax = inactiviteMax;
    }

    public Duration getDureeMax() {
        return dureeMax;
    }

    public void setDureeMax(Duration dureeMax) {
        this.dureeMax = dureeMax;
    }

    public Duration getBalayage() {
        return balayage;
    }

    public void setBalayage(Duration balayage) {
        this.balayage = balayage;
    }

    public String getTemoin() {
        return temoin;
    }

    public void setTemoin(String temoin) {
        this.temoin = temoin;
    }
}
