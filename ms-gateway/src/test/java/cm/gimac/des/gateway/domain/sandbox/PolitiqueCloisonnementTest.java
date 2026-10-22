package cm.gimac.des.gateway.domain.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.gateway.domain.sandbox.DecisionSandbox.Motif;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Cloisonnement par institution étendu aux mandats (US-EXI-07, CC-SBX §6.1.1), routes et décisions. */
class PolitiqueCloisonnementTest {

    static final Instant T = Instant.parse("2026-10-22T10:00:00Z");
    static final PerimetreMandats PARTENAIRE = new PerimetreMandats(
        "u-partenaire",
        "99010",
        Map.of("99001", Instant.parse("2026-11-10T00:00:00Z"), "99002", Instant.parse("2026-11-10T00:00:00Z"), "99003", T),
        9
    );

    @Test
    @DisplayName("Critère 1 : un développeur de 99001 sans choix agit au nom de son institution")
    void institutionPropre() {
        DecisionSandbox d = PolitiqueCloisonnement.decider("99001", null, null, T);
        assertThat(d).isEqualTo(new DecisionSandbox(true, "99001", Motif.INSTITUTION_PROPRE));
        assertThat(PolitiqueCloisonnement.decider("99001", " ", null, T).motif()).isEqualTo(Motif.INSTITUTION_PROPRE);
        assertThat(PolitiqueCloisonnement.decider("99001", "99001", null, T).institutionEffective()).isEqualTo("99001");
    }

    @Test
    @DisplayName("Critère 2 : partenaire mandaté par 99001 et 99002, institution mandante 99002 tant que le mandat est actif")
    void partenaireMandate() {
        DecisionSandbox d = PolitiqueCloisonnement.decider("99010", "99002", PARTENAIRE, T);
        assertThat(d).isEqualTo(new DecisionSandbox(true, "99002", Motif.MANDAT_ACTIF));
        assertThat(PolitiqueCloisonnement.decider("99010", "99001", PARTENAIRE, T).institutionEffective()).isEqualTo("99001");
    }

    @Test
    @DisplayName("Critère 4 : mandat échu (fin atteinte) ou absent, refus au nom de l'institution du jeton")
    void horsMandat() {
        assertThat(PolitiqueCloisonnement.decider("99010", "99003", PARTENAIRE, T)).isEqualTo(new DecisionSandbox(false, "99010", Motif.MANDAT_ECHU));
        assertThat(PolitiqueCloisonnement.decider("99010", "99004", PARTENAIRE, T).motif()).isEqualTo(Motif.MANDAT_ABSENT);
        assertThat(PolitiqueCloisonnement.decider("99010", "99002", null, T).motif()).isEqualTo(Motif.MANDAT_ABSENT);
        assertThat(PolitiqueCloisonnement.decider("99010", "99002", PARTENAIRE, Instant.parse("2026-11-10T00:00:00Z")).motif()).isEqualTo(Motif.MANDAT_ECHU);
    }

    @Test
    @DisplayName("Compte du GIMAC (aucune institution) : admis sans institution, le choix est ignoré")
    void compteGimac() {
        assertThat(PolitiqueCloisonnement.decider(null, "99002", null, T)).isEqualTo(new DecisionSandbox(true, "", Motif.COMPTE_GIMAC));
        assertThat(PolitiqueCloisonnement.decider("", null, null, T).admise()).isTrue();
    }

    @Test
    @DisplayName("Motifs : admis ou refus ; décision sans institution normalisée")
    void motifs() {
        assertThat(Motif.values()).filteredOn(Motif::admis).containsExactly(Motif.COMPTE_GIMAC, Motif.INSTITUTION_PROPRE, Motif.MANDAT_ACTIF);
        assertThat(DecisionSandbox.de(Motif.QUOTA_DEPASSE, null)).isEqualTo(new DecisionSandbox(false, "", Motif.QUOTA_DEPASSE));
    }

    @Test
    @DisplayName("Périmètre : compte exigé, institution propre vide par défaut, mandantes copiées")
    void perimetre() {
        PerimetreMandats p = new PerimetreMandats("u", null, Map.of(), 1);
        assertThat(p.institutionPropre()).isEmpty();
        assertThat(p.finMandat("99001")).isNull();
        assertThatThrownBy(() -> new PerimetreMandats(null, "", Map.of(), 1)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Routes : onze services, trois couches, module de la matrice ; tout autre chemin n'est pas une route de la sandbox")
    void routes() {
        assertThat(RouteSandbox.MODULES).hasSize(11);
        assertThat(RouteSandbox.lire("/bff/portail/sbx-simulation/essais")).contains(new RouteSandbox("portail", "simulation"));
        RouteSandbox r = RouteSandbox.lire("/bff/des/sbx-espace").orElseThrow();
        assertThat(r.nomService()).isEqualTo("sbx-espace");
        assertThat(r.module()).isEqualTo("HAB");
        assertThat(RouteSandbox.lire("/bff/console/sbx-campagne/x/y").orElseThrow().module()).isEqualTo("CER");
        assertThat(RouteSandbox.lire("/bff/portail/sbx-habilitations/comptes")).isEmpty();
        assertThat(RouteSandbox.lire("/bff/autre/sbx-espace/x")).isEmpty();
        assertThat(RouteSandbox.lire(null)).isEmpty();
    }

    @Test
    @DisplayName("Jeton réduit : permissions copiées")
    void jetonReduit() {
        java.util.List<String> l = new java.util.ArrayList<>(java.util.List.of("SBX_SIM_SIMULATION"));
        JetonReduit j = new JetonReduit("u", "99001", "99001", l, "2", "sbx-simulation");
        l.add("X");
        assertThat(j.permissions()).containsExactly("SBX_SIM_SIMULATION");
    }
}
