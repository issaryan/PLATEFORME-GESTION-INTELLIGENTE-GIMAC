package cm.gimac.des.homologation.domain.parcours;

import static cm.gimac.des.homologation.domain.parcours.ModeleParcoursTest.saisie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.EtapeReferentiel;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.service.VisibiliteReferentiel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Étapes configurables et visibilité du référentiel (D-89, docs/lot1/referentiel-droits-etapes.md). */
class EtapesConfigurablesTest {

    static ReferentialVersion brouillon() {
        ReferentialVersion v = ReferentialVersion.nouveauBrouillon(TypeProjet.DELEGATAIRE, "Essai", Instant.EPOCH);
        v.setId(UUID.randomUUID());
        return v;
    }

    static void code(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    /** Saisie rangée dans l'étape {@code etape} de la version (code libre). */
    static ReferenceOperationData dans(String etape, Fonction r, Fonction v, String... preds) {
        ReferenceOperationData d = saisie("Op", EtapeCode.CADRAGE, Segment.TRONC_COMMUN_AMONT, r, v, 2, Geste.DEPOSER_PREUVE, preds);
        return new ReferenceOperationData(
            d.libelle(), d.phase(), d.segment(), d.partie(), d.serviceCode(), d.natureCouverture(), d.preuveAttendue(), d.idScenario(),
            d.predecesseurs(), d.fonctionResponsable(), d.grille(), d.code(), d.description(), etape, d.ordre(), d.responsable(),
            d.validateur(), d.consultes(), List.of(), List.of(), d.delaiJours(), d.grain(), d.plateforme(), d.cle(), d.secret(),
            d.oubliSansSymptome(), d.pivot(), d.valeurReference(), d.livrables(), d.geste(), d.jalon(), d.declencheur(), d.position(),
            d.particularite()
        );
    }

    @Test
    @DisplayName("Nouveau brouillon : les onze étapes actuelles, chacune liée à sa canonique, dans l'ordre du CR")
    void etapesInitiales() {
        ReferentialVersion v = brouillon();
        assertThat(v.etapes()).extracting(EtapeReferentiel::getCode).containsExactly(
            "PRISE_CONTACT", "ADHESION", "CADRAGE", "LANCEMENT", "ATELIERS", "INTEGRATION", "TESTS", "CERTIFICATION",
            "APPROBATION_SECURITE", "MISE_EN_PRODUCTION", "SUIVI"
        );
        assertThat(v.etapes()).allSatisfy(e -> assertThat(e.getCanonique()).isEqualTo(EtapeCode.valueOf(e.getCode())));
        assertThat(v.etapes()).extracting(EtapeReferentiel::getOrdre).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        assertThat(v.etape("APPROBATION_SECURITE").orElseThrow().protegee()).isTrue();
        assertThat(v.etape("TESTS").orElseThrow().protegee()).isFalse();
        assertThat(v.etape("CERTIFICATION").orElseThrow().getCouleur()).isEqualTo(CouleurEtape.SUCCES);
        assertThat(java.util.Arrays.stream(EtapeCode.values()).map(EtapeCode::couleur)).doesNotContainNull();
        assertThat(java.util.Arrays.stream(EtapeCode.values()).filter(EtapeCode::protegee)).containsExactly(
            EtapeCode.CERTIFICATION,
            EtapeCode.APPROBATION_SECURITE,
            EtapeCode.MISE_EN_PRODUCTION
        );
        assertThat(EtapeCode.de(" TESTS ")).contains(EtapeCode.TESTS);
        assertThat(EtapeCode.de(null)).isEmpty();
        assertThat(java.util.Arrays.stream(CouleurEtape.values())).allSatisfy(c -> {
            assertThat(c.libelle()).isNotBlank();
            assertThat(c.jeton()).startsWith("--gm-color-");
        });
    }

    @Test
    @DisplayName("Ajout, renommage, couleur, ordre ; codes invalides ou dupliqués refusés")
    void ajouterModifierOrdonner() {
        ReferentialVersion v = brouillon();
        EtapeReferentiel e = v.ajouterEtape("REVUE_JURIDIQUE", " Revue juridique ", CouleurEtape.ACCENT, null, 8);
        assertThat(e.getOrdre()).isEqualTo(8);
        assertThat(e.getLibelle()).isEqualTo("Revue juridique");
        assertThat(v.etape("CERTIFICATION").orElseThrow().getOrdre()).isEqualTo(9);
        assertThat(v.etapes()).hasSize(12);
        assertThat(v.ajouterEtape("CLOTURE", "Clôture", null, null, null).getOrdre()).isEqualTo(13);
        code(() -> v.ajouterEtape("REVUE_JURIDIQUE", "Doublon", null, null, null), "HOM_ETAPE_CODE_DUPLIQUE");
        code(() -> v.ajouterEtape("revue", "Minuscules", null, null, null), "HOM_ETAPE_CODE_INVALIDE");
        code(() -> v.ajouterEtape("X", "Trop court", null, null, null), "HOM_ETAPE_CODE_INVALIDE");
        code(() -> v.ajouterEtape("_X", "Souligné en tête", null, null, null), "HOM_ETAPE_CODE_INVALIDE");
        code(() -> v.ajouterEtape("VIDE", "  ", null, null, null), "HOM_ETAPE_LIBELLE_INVALIDE");
        code(() -> v.ajouterEtape("LONG", "L".repeat(121), null, null, null), "HOM_ETAPE_LIBELLE_INVALIDE");

        assertThat(v.modifierEtape("REVUE_JURIDIQUE", "Revue juridique et conformité", CouleurEtape.INFO, EtapeCode.TESTS).getCanonique()).isEqualTo(EtapeCode.TESTS);
        assertThat(v.modifierEtape("TESTS", "Essais", null, EtapeCode.TESTS).getCouleur()).isEqualTo(CouleurEtape.ALERTE);
        code(() -> v.modifierEtape("CERTIFICATION", "Certification", null, null), "HOM_ETAPE_CANONIQUE");
        code(() -> v.modifierEtape("APPROBATION_SECURITE", "Verrou 2", null, EtapeCode.TESTS), "HOM_ETAPE_CANONIQUE");
        code(() -> v.modifierEtape("INCONNUE", "x", null, null), "HOM_ETAPE_NOT_FOUND");

        List<String> ordre = new ArrayList<>(v.etapes().stream().map(EtapeReferentiel::getCode).toList());
        java.util.Collections.reverse(ordre);
        assertThat(v.ordonnerEtapes(ordre)).extracting(EtapeReferentiel::getCode).containsExactlyElementsOf(ordre);
        code(() -> v.ordonnerEtapes(ordre.subList(1, ordre.size())), "HOM_ETAPE_ORDRE_INVALIDE");
        code(() -> v.ordonnerEtapes(List.of()), "HOM_ETAPE_ORDRE_INVALIDE");
    }

    @Test
    @DisplayName("Retrait : étape vide seulement ; étape liée à une canonique porteuse d'un jalon ou du verrou 2 jamais")
    void retirer() {
        ReferentialVersion v = brouillon();
        v.ajouterEtape("REVUE", "Revue", null, null, 8);
        v.upsertOperation("A", dans("REVUE", Fonction.SNA, Fonction.CONDUCTEUR));
        code(() -> v.retirerEtape("REVUE"), "HOM_ETAPE_NON_VIDE");
        code(() -> v.retirerEtape("CERTIFICATION"), "HOM_ETAPE_CANONIQUE");
        code(() -> v.retirerEtape("APPROBATION_SECURITE"), "HOM_ETAPE_CANONIQUE");
        code(() -> v.retirerEtape("MISE_EN_PRODUCTION"), "HOM_ETAPE_CANONIQUE");
        assertThat(v.retirerEtape("SUIVI").getCode()).isEqualTo("SUIVI");
        v.retirerOperation("A");
        v.retirerEtape("REVUE");
        assertThat(v.etapes()).extracting(EtapeReferentiel::getOrdre).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        code(() -> v.upsertOperation("B", dans("SUIVI", Fonction.SNA, Fonction.CONDUCTEUR)), "HOM_ETAPE_INCONNUE");
    }

    @Test
    @DisplayName("Progression : étape sans canonique rattachée à la canonique de l'étape précédente ; avant le cadrage, au cadrage")
    void progressionParLaCanonique() {
        ReferentialVersion v = brouillon();
        v.ajouterEtape("REVUE", "Revue", null, null, 8); // après TESTS
        v.ajouterEtape("ACCUEIL", "Accueil", null, null, 3); // après ADHESION, hors parcours
        v.ajouterEtape("AMORCE", "Amorce", null, null, 1); // première étape
        assertThat(v.canoniqueDe("REVUE")).contains(EtapeCode.TESTS);
        assertThat(v.canoniqueDe("ACCUEIL")).contains(EtapeCode.CADRAGE);
        assertThat(v.canoniqueDe("AMORCE")).contains(EtapeCode.CADRAGE);
        assertThat(v.canoniqueDe("TESTS")).contains(EtapeCode.TESTS);
        assertThat(v.canoniqueDe("INCONNUE")).isEmpty();

        v.upsertOperation("A", dans("CADRAGE", Fonction.DDRM, Fonction.CONDUCTEUR));
        v.upsertOperation("B", dans("REVUE", Fonction.SNA, Fonction.CONDUCTEUR, "A"));
        v.upsertOperation("C", dans("ACCUEIL", Fonction.SNA, Fonction.CONDUCTEUR));
        ReferenceOperation b = v.operation("B").orElseThrow();
        assertThat(b.getEtape()).isEqualTo("REVUE");
        assertThat(b.getPhase()).isEqualTo("REVUE");
        assertThat(b.etapeEffective()).isEqualTo(EtapeCode.TESTS);
        assertThat(v.operation("C").orElseThrow().etapeEffective()).isEqualTo(EtapeCode.CADRAGE);

        var l = ParcoursSamples.liaison(TypeProjet.DELEGATAIRE);
        Parcours p = Parcours.instancier(l, new ParcoursLiaison(l.getId(), "LIA-2026-089", ParcoursSamples.DEBUT), v.getOperations(), List.of(), ParcoursSamples.designation(l));
        assertThat(p.operation("B").orElseThrow().getEtape()).isEqualTo(EtapeCode.TESTS);
        assertThat(p.operation("C").orElseThrow().getEtape()).isEqualTo(EtapeCode.CADRAGE);
    }

    @Test
    @DisplayName("Dérivation : les étapes de la source sont copiées ; version figée : étapes non modifiables")
    void derivationEtGel() {
        ReferentialVersion v = brouillon();
        v.ajouterEtape("REVUE", "Revue", CouleurEtape.ACCENT, null, 8);
        v.upsertOperation("A", dans("REVUE", Fonction.SNA, Fonction.CONDUCTEUR));
        v.publier(1, Instant.EPOCH, List.of("A"));
        code(() -> v.ajouterEtape("AUTRE", "Autre", null, null, null), "HOM_REFERENTIAL_FROZEN");
        code(() -> v.retirerEtape("SUIVI"), "HOM_REFERENTIAL_FROZEN");
        code(() -> v.modifierEtape("REVUE", "x", null, null), "HOM_REFERENTIAL_FROZEN");
        code(() -> v.ordonnerEtapes(List.of()), "HOM_REFERENTIAL_FROZEN");

        ReferentialVersion d = ReferentialVersion.deriverDe(v, "v2", Instant.EPOCH);
        assertThat(d.etapes()).extracting(EtapeReferentiel::getCode).containsExactlyElementsOf(v.etapes().stream().map(EtapeReferentiel::getCode).toList());
        assertThat(d.etapes()).allSatisfy(e -> assertThat(e.getVersion()).isSameAs(d));
        assertThat(d.etape("REVUE").orElseThrow().getCouleur()).isEqualTo(CouleurEtape.ACCENT);
        assertThat(d.operation("A").orElseThrow().etapeEffective()).isEqualTo(EtapeCode.TESTS);
        assertThat(d.actionsPermises()).contains("MODIFIER", "CONTROLER", "PUBLIER");
    }

    @Test
    @DisplayName("Visibilité : éditeurs et READ_ALL voient tout ; un lecteur concerné, ses opérations et leurs voisines en fantômes")
    void visibilite() {
        ReferentialVersion v = brouillon();
        v.upsertOperation("A", dans("CADRAGE", Fonction.DDRM, Fonction.CONDUCTEUR));
        v.upsertOperation("B", dans("LANCEMENT", Fonction.POLE_RESEAU, Fonction.CONDUCTEUR, "A"));
        v.upsertOperation("C", dans("TESTS", Fonction.SNA, Fonction.CONDUCTEUR, "B"));
        v.upsertOperation("D", dans("TESTS", Fonction.POLE_RESEAU, Fonction.CONDUCTEUR, "C"));
        v.upsertOperation("E", dans("TESTS", Fonction.POLE_SYSTEME, Fonction.CONDUCTEUR, "D"));
        v.upsertOperation("H", dans("TESTS", Fonction.RSSI, Fonction.CONDUCTEUR, "A"));
        List<ReferenceOperation> ops = v.getOperations();

        assertThat(VisibiliteReferentiel.pour(Set.of("HOM_REFERENTIEL_READ", "HOM_REFERENTIEL_READ_ALL"), ops).filtre()).isEqualTo(VisibiliteReferentiel.Filtre.COMPLET);
        assertThat(VisibiliteReferentiel.pour(Set.of("HOM_REFERENTIEL_EDIT"), ops).visibles()).hasSize(6);

        var sna = VisibiliteReferentiel.pour(Set.of("HOM_REFERENTIEL_READ", "ROLE_SNA"), ops);
        assertThat(sna.filtre()).isEqualTo(VisibiliteReferentiel.Filtre.CONCERNE);
        assertThat(sna.visibles()).containsExactly("C");
        assertThat(sna.fantomes()).containsExactlyInAnyOrder("B", "D");

        var aucun = VisibiliteReferentiel.pour(Set.of("HOM_REFERENTIEL_READ", "ROLE_COLLABORATEUR"), ops);
        assertThat(aucun.visibles()).isEmpty();
        assertThat(aucun.fantomes()).isEmpty();

        // Le conducteur valide (V) toutes les opérations ; le chef du DES tient aussi la fonction collective.
        assertThat(VisibiliteReferentiel.pour(Set.of("ROLE_CONDUCTEUR_PROJET"), ops).visibles()).hasSize(6);
        assertThat(VisibiliteReferentiel.fonctions(Set.of("ROLE_CHEF_DDRM"))).contains(Fonction.CHEF_DDRM, Fonction.CHEFS_DEPARTEMENT);
        ReferenceOperation c = v.operation("C").orElseThrow();
        assertThat(VisibiliteReferentiel.concerne(c, Set.of(Fonction.PARTICIPANT))).isFalse();
        assertThat(VisibiliteReferentiel.concerne(c, Set.of())).isFalse();
    }
}
