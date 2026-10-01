package cm.gimac.des.gateway.infrastructure.appareils;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.IntegrationTest;
import cm.gimac.des.gateway.domain.appareils.Appareil;
import cm.gimac.des.gateway.domain.appareils.ResultatTentative;
import cm.gimac.des.gateway.domain.appareils.TentativeConnexion;
import cm.gimac.des.gateway.domain.appareils.TypeAppareil;
import cm.gimac.des.gateway.domain.appareils.port.HistoriqueConnexions;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

/**
 * Historique des connexions dans la base PostgreSQL de la passerelle (Testcontainers, table Liquibase
 * historique_connexion, R2DBC) : écriture, lecture par sujet ou empreinte d'identifiant, ordre et
 * pagination, appareils connus, purge à 90 jours, contrainte sur les résultats.
 */
@IntegrationTest
class HistoriqueConnexionsR2dbcIT {

    @Autowired
    HistoriqueConnexions historique;

    @Autowired
    DatabaseClient base;

    @Test
    @DisplayName("Adaptateur R2DBC choisi, écriture et lecture, pagination, appareils connus, purge, contrainte")
    void historique() {
        assertThat(historique).isInstanceOf(HistoriqueConnexionsR2dbc.class);
        String sujet = "u-" + UUID.randomUUID();
        String empreinte = UUID.randomUUID().toString().replace("-", "");
        Instant maintenant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Appareil firefox = new Appareil("Firefox 131", "Windows 10/11", TypeAppareil.ORDINATEUR);
        Appareil iphone = new Appareil("Safari 17", "iOS 17", TypeAppareil.MOBILE);
        historique.enregistrer(new TentativeConnexion(maintenant.minus(Duration.ofDays(100)), sujet, empreinte, ResultatTentative.REUSSIE, firefox, "10.0.0.1", "des")).block();
        historique.enregistrer(new TentativeConnexion(maintenant.minusSeconds(30), sujet, empreinte, ResultatTentative.REUSSIE, firefox, "41.202.10.20", "des")).block();
        historique.enregistrer(new TentativeConnexion(maintenant.minusSeconds(20), null, empreinte, ResultatTentative.ECHEC_MOT_DE_PASSE, iphone, null, null)).block();
        historique.enregistrer(new TentativeConnexion(maintenant.minusSeconds(10), sujet, empreinte, ResultatTentative.ECHEC_CODE, iphone, "41.202.10.21", "portail")).block();
        historique.enregistrer(new TentativeConnexion(maintenant, "autre", "autre", ResultatTentative.BLOQUEE, iphone, "1.1.1.1", "des")).block();

        HistoriqueConnexions.Page p = historique.lire(sujet, Set.of(empreinte), 0, 2).block();
        assertThat(p.total()).isEqualTo(4);
        assertThat(p.contenu()).extracting(TentativeConnexion::resultat).containsExactly(ResultatTentative.ECHEC_CODE, ResultatTentative.ECHEC_MOT_DE_PASSE);
        assertThat(p.contenu().get(0).le()).isEqualTo(maintenant.minusSeconds(10));
        assertThat(p.contenu().get(0).appareil()).isEqualTo(iphone);
        assertThat(p.contenu().get(0).couche()).isEqualTo("portail");
        assertThat(p.contenu().get(1).sujet()).isNull();
        assertThat(p.contenu().get(1).adresseIp()).isNull();
        // Par le sujet seul : l'échec du mot de passe (sans sujet) n'y est pas ; page 1 de 2 : la plus ancienne.
        HistoriqueConnexions.Page parSujet = historique.lire(sujet, Set.of(), 1, 2).block();
        assertThat(parSujet.total()).isEqualTo(3);
        assertThat(parSujet.contenu()).extracting(TentativeConnexion::resultat).containsExactly(ResultatTentative.REUSSIE);
        assertThat(parSujet.contenu().get(0).le()).isEqualTo(maintenant.minus(Duration.ofDays(100)));
        assertThat(historique.lire(null, null, 0, 10).block().total()).isZero();
        List<Appareil> connus = historique.appareilsConnus(sujet).collectList().block();
        assertThat(connus).containsExactly(firefox);

        long purgees = historique.purger(maintenant.minus(Duration.ofDays(90))).block();
        assertThat(purgees).isGreaterThanOrEqualTo(1);
        assertThat(historique.lire(sujet, Set.of(empreinte), 0, 10).block().total()).isEqualTo(3);
        assertThat(HistoriqueConnexionsR2dbc.type("AUTRE")).isEqualTo(TypeAppareil.INCONNU);
        assertThat(HistoriqueConnexionsR2dbc.type(null)).isEqualTo(TypeAppareil.INCONNU);
        // Contrainte : un résultat hors du contrat est refusé par la base.
        Throwable refus = base
            .sql("insert into historique_connexion (id, le, resultat, navigateur, systeme, type_appareil) values (:id, now(), 'AUTRE', 'x', 'y', 'INCONNU')")
            .bind("id", UUID.randomUUID())
            .then()
            .then(Mono.<Throwable>empty())
            .onErrorResume(Mono::just)
            .block();
        assertThat(refus).isNotNull();
    }
}
