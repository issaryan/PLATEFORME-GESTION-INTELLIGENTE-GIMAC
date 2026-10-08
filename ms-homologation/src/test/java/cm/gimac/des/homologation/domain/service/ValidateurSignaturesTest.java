package cm.gimac.des.homologation.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.parcours.CatalogueFonctions;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.SignatureOperation;
import cm.gimac.des.homologation.domain.parcours.SignaturesSamples;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator.Anomalie;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator.TypeAnomalie;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Contrôles de publication des signatures et du catalogue des fonctions (D-91, docs/lot1/types-projet.md §3). */
class ValidateurSignaturesTest {

    @BeforeEach
    void init() {
        SignaturesSamples.brancherCatalogue();
    }

    @AfterEach
    void fin() {
        CatalogueFonctions.brancher(null);
    }

    static List<TypeAnomalie> types(ReferentialVersion v) {
        return new ReferentialGraphValidator().controler(v.noeuds()).anomalies().stream().map(Anomalie::type).toList();
    }

    @Test
    @DisplayName("Version conforme : signataires au catalogue, livrables portés de l'opération et d'une opération antérieure")
    void conforme() {
        assertThat(types(SignaturesSamples.version(SignaturesSamples.signatures()))).isEmpty();
        assertThat(types(SignaturesSamples.version(List.of()))).isEmpty();
    }

    @Test
    @DisplayName("Anomalies : code dupliqué, signataire inconnu ou externe, sans livrable, livrable inconnu ou non antérieur, fonction inconnue")
    void anomalies() {
        List<SignatureOperation> s = List.of(
            new SignatureOperation("SA", "A", Fonction.CONDUCTEUR, 1, List.of("L1")),
            new SignatureOperation("SA", "A bis", Fonction.CONDUCTEUR, 2, List.of("L1")),
            new SignatureOperation("SB", "B", Fonction.valueOf("INCONNUE_X"), 1, List.of("L1")),
            new SignatureOperation("SC", "C", Fonction.PARTICIPANT, 1, List.of("L1")),
            new SignatureOperation("SD", "D", Fonction.CONDUCTEUR, 1, List.of()),
            new SignatureOperation("SE", "E", Fonction.CONDUCTEUR, 1, List.of("L9", "AUTRE/L1", "OUV/L9"))
        );
        List<TypeAnomalie> t = types(SignaturesSamples.version(s));
        assertThat(t).contains(
            TypeAnomalie.SIGNATURE_DUPLIQUEE,
            TypeAnomalie.SIGNATAIRE_INCONNU,
            TypeAnomalie.SIGNATAIRE_EXTERNE,
            TypeAnomalie.SIGNATURE_SANS_LIVRABLE,
            TypeAnomalie.SIGNATURE_PORTEE_INVALIDE,
            TypeAnomalie.FONCTION_INCONNUE
        );
        assertThat(t.stream().filter(x -> x == TypeAnomalie.SIGNATURE_PORTEE_INVALIDE)).hasSize(3);
        // Livrable d'une opération qui suit (non antérieure) : refusé.
        ReferentialVersion v = SignaturesSamples.version(List.of());
        v.upsertOperation(
            "OUV",
            SignaturesSamples.donnees("LANCEMENT", Fonction.CONDUCTEUR, Fonction.CHEF_DES, List.of(), List.of(), List.of(new SignatureOperation("SS", "S", Fonction.CONDUCTEUR, 1, List.of("SIG/L1"))))
        );
        assertThat(types(v)).containsExactly(TypeAnomalie.SIGNATURE_PORTEE_INVALIDE);
        // Fonction désactivée : absente du catalogue actif.
        CatalogueFonctions.retenir(new CatalogueFonctions.Definition("JURISTE", "Juriste", List.of("ROLE_JURISTE"), false, false));
        assertThat(types(SignaturesSamples.version(SignaturesSamples.signatures()))).contains(TypeAnomalie.SIGNATAIRE_INCONNU, TypeAnomalie.FONCTION_INCONNUE);
    }

    @Test
    @DisplayName("Saisie : forme des signatures contrôlée à l'écriture (422 HOM_SIGNATURE_INVALIDE, champ en cause)")
    void saisie() {
        ReferentialVersion v = SignaturesSamples.version(List.of());
        List<List<SignatureOperation>> invalides = Arrays.asList(
            Arrays.asList((SignatureOperation) null),
            List.of(new SignatureOperation("a", "A", Fonction.DG, 1, List.of("L1"))),
            List.of(new SignatureOperation("SA", " ", Fonction.DG, 1, List.of("L1"))),
            List.of(new SignatureOperation("A", null, Fonction.DG, 1, List.of("L1"))),
            List.of(new SignatureOperation("SA", "x".repeat(201), Fonction.DG, 1, List.of("L1"))),
            List.of(new SignatureOperation("SA", "A", null, 1, List.of("L1"))),
            List.of(new SignatureOperation("SA", "A", Fonction.DG, 0, List.of("L1"))),
            List.of(new SignatureOperation("SA", "A", Fonction.DG, 100, List.of("L1"))),
            List.of(new SignatureOperation("SA", "A", Fonction.DG, 1, Arrays.asList("L1", " "))),
            List.of(new SignatureOperation("SA", "A", Fonction.DG, 1, java.util.Collections.nCopies(31, "L1"))),
            java.util.Collections.nCopies(31, new SignatureOperation("SA", "A", Fonction.DG, 1, List.of("L1")))
        );
        for (List<SignatureOperation> s : invalides) {
            assertThatThrownBy(() -> v.upsertOperation("SIG", SignaturesSamples.donnees("LANCEMENT", Fonction.CONDUCTEUR, Fonction.CHEF_DES, List.of(), List.of("OUV"), s)))
                .isInstanceOfSatisfying(HomologationException.class, e -> {
                    assertThat(e.code()).isEqualTo("HOM_SIGNATURE_INVALIDE");
                    assertThat(e.status()).isEqualTo(422);
                    assertThat(e.details()).containsKey("champ");
                });
        }
        // Signatures absentes de la saisie : celles de l'opération sont gardées ; liste vide : retirées.
        ReferentialVersion w = SignaturesSamples.version(SignaturesSamples.signatures());
        w.upsertOperation("SIG", SignaturesSamples.donnees("LANCEMENT", Fonction.CONDUCTEUR, Fonction.CHEF_DES, List.of(), List.of("OUV"), null));
        assertThat(w.operation("SIG").orElseThrow().getSignatures()).hasSize(2);
        assertThat(w.operation("SIG").orElseThrow().fonctionsCitees()).contains(SignaturesSamples.JURISTE, Fonction.CONDUCTEUR, Fonction.CHEF_DES);
        w.upsertOperation("SIG", SignaturesSamples.donnees("LANCEMENT", Fonction.CONDUCTEUR, Fonction.CHEF_DES, List.of(), List.of("OUV"), List.of()));
        assertThat(w.operation("SIG").orElseThrow().getSignatures()).isEmpty();
        // Copie vers un autre type (départ COPIE) : signatures comprises.
        ReferentialVersion c = ReferentialVersion.copierVers(SignaturesSamples.version(SignaturesSamples.signatures()), cm.gimac.des.homologation.domain.enumeration.TypeProjet.valueOf("COPIE_X"), "Copie", java.time.Instant.now());
        assertThat(c.getTypeProjet().name()).isEqualTo("COPIE_X");
        assertThat(c.getVersionSourceId()).isNull();
        assertThat(c.operation("SIG").orElseThrow().getSignatures()).hasSize(2);
        assertThat(c.etapes()).hasSize(11);
    }
}
