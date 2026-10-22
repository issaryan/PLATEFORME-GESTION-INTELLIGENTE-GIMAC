package cm.gimac.des.iam.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.domain.compte.ActionRequise;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.EtatConnexion;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** État de connexion effectif lu par la passerelle (D-119, US-EXI-06), sans Docker. */
class EtatEffectifSandboxTest {

    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");

    static Compte compte(String id, String institution, boolean actif) {
        return new Compte(id, id, "P", "N", id + "@t.test", true, actif, T, null, null, "fr", institution, true, Set.of(), List.of(), true);
    }

    @Test
    @DisplayName("US-EXI-06 scénarios 1 et 2 : compte d'un périmètre suspendu vu inactif, son état restitué à la levée ; connexion admise relevée")
    void etatEffectif() {
        Memoires.Perimetres perimetres = new Memoires.Perimetres();
        Memoires.Activite activite = new Memoires.Activite();
        EtatEffectifSandbox etat = new EtatEffectifSandbox(perimetres, activite, new Memoires.Horloge(T));
        Compte membre = compte("c1", "99001", true);
        Compte interne = compte("c2", null, true);
        Compte inactif = compte("c3", "99001", false);

        EtatConnexion e = etat.etat(membre);
        assertThat(e.actif()).isTrue();
        assertThat(e.secondFacteur()).isTrue();
        assertThat(e.actionsRequises()).isEmpty();
        assertThat(activite.lignes).containsEntry("c1", T);

        perimetres.poser("99001", "sa", null, T);
        assertThat(etat.etat(membre).actif()).isFalse();
        assertThat(etat.etat(interne).actif()).isTrue();
        assertThat(etat.etat(inactif).actif()).isFalse();
        assertThat(activite.lignes).containsOnlyKeys("c1", "c2");

        perimetres.lever("99001");
        assertThat(etat.etat(membre).actif()).isTrue();
        assertThat(etat.etat(inactif).actif()).isFalse();
        assertThat(etat.etat(compte("c4", "99001", true)).actionsRequises()).doesNotContain(ActionRequise.SECOND_FACTEUR);
    }
}
