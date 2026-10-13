package cm.gimac.des.homologation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.enumeration.NatureCouverture;
import cm.gimac.des.homologation.domain.enumeration.Partie;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Declencheurs;
import cm.gimac.des.homologation.domain.parcours.EtapeCode;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.GrainPreuve;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.Livrable;
import cm.gimac.des.homologation.domain.parcours.NatureLivrable;
import cm.gimac.des.homologation.domain.parcours.Particularite;
import cm.gimac.des.homologation.domain.parcours.PlateformeExecution;
import cm.gimac.des.homologation.domain.parcours.Position;
import cm.gimac.des.homologation.domain.parcours.SegmentParcours;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator.TypeAnomalie;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Modélisateur (D-77) : écriture d'un brouillon, valeurs effectives d'une opération, contrôles du modèle (§2). */
class ReferentialModelisateurTest {

    static ReferentialVersion brouillon() {
        ReferentialVersion v = ReferentialVersion.nouveauBrouillon(TypeProjet.DELEGATAIRE, "Essai", Instant.EPOCH);
        v.setId(UUID.randomUUID());
        return v;
    }

    static ReferenceOperationData op(Fonction r, Fonction v, Geste geste, Jalon jalon, String declencheur, List<Livrable> livrables, String... preds) {
        return new ReferenceOperationData(
            "Libellé",
            null,
            Segment.TRONC_COMMUN_AMONT,
            null,
            null,
            null,
            null,
            null,
            List.of(preds),
            null,
            null,
            null,
            null,
            "CADRAGE",
            1,
            r,
            v,
            List.of(Fonction.SNA, Fonction.SNA),
            List.of(),
            List.of(Fonction.PARTICIPANT),
            2,
            GrainPreuve.PHASE,
            PlateformeExecution.SWITCH_DELEGATAIRE,
            true,
            true,
            false,
            true,
            null,
            livrables,
            geste,
            jalon,
            declencheur,
            new Position(10, 20),
            Particularite.FIN_ESSAIS
        );
    }

    /** Saisie réduite aux délais cible et maximal (D-106). */
    static ReferenceOperationData delais(Integer cible, Integer max) {
        return new ReferenceOperationData(
            "x", null, null, null, null, null, null, null, List.of(), null, null, null, null, null, null, null, null, null, null, null,
            cible,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            max
        );
    }

    static ReferenceOperationData simple(String... preds) {
        return op(Fonction.DDRM, Fonction.CONDUCTEUR, Geste.DEPOSER_PREUVE, null, null, List.of(), preds);
    }

    static void code(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    @DisplayName("Création au code fourni, code pris, code mal formé, secret avec valeur, autoréférence ; retrait des prédécesseurs")
    void creationEtRetrait() {
        ReferentialVersion v = brouillon();
        ReferenceOperation a = v.creerOperation("A", simple());
        v.creerOperation("B", simple("A"));
        v.creerOperation("C", simple("A", "B"));
        code(() -> v.creerOperation("A", simple()), "HOM_OPERATION_CODE_DUPLICATE");
        code(() -> v.creerOperation("a-minuscule", simple()), "HOM_OPERATION_CODE_INVALID");
        code(() -> v.creerOperation(null, simple()), "HOM_OPERATION_CODE_INVALID");
        code(() -> v.creerOperation("Z".repeat(41), simple()), "HOM_OPERATION_CODE_INVALID");
        ReferenceOperationData secretValeur = new ReferenceOperationData(
            "x", null, null, null, null, null, null, null, List.of(), null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, true, null, null, "Code du membre", null, null, null, null, null, null
        );
        code(() -> v.creerOperation("S", secretValeur), "HOM_OPERATION_SECRET_VALUE");
        code(() -> v.creerOperation("D", simple("D")), "HOM_OPERATION_SELF_PREDECESSOR");
        // D-106 : délai maximal au moins égal au délai cible.
        code(() -> v.creerOperation("M", delais(10, 5)), "HOM_OPERATION_DELAI_MAX_INVALIDE");
        ReferenceOperation m = v.creerOperation("M", delais(10, 15));
        assertThat(m.getDelaiJours()).isEqualTo(10);
        assertThat(m.getDelaiMaxJours()).isEqualTo(15);
        assertThat(m.delaiMaxEffectif()).isEqualTo(15);
        assertThat(v.creerOperation("N", delais(4, null)).delaiMaxEffectif()).isEqualTo(4);
        assertThat(a.getPosition()).isEqualTo(new Position(10, 20));
        assertThat(a.getConsultes()).containsExactly(Fonction.SNA);
        assertThat(a.getPartie()).isEqualTo(Partie.DES);
        assertThat(a.getPhase()).isEqualTo("CADRAGE");
        assertThat(a.getServiceCode()).isEqualTo("TRANSVERSE");
        assertThat(a.getNatureCouverture()).isEqualTo(NatureCouverture.AUCUNE);
        assertThat(a.getFonctionResponsable()).isEqualTo("DDRM");
        assertThat(JsonTexte.objet(a.getPreuveAttendue())).containsEntry("grain", "PHASE").containsEntry("cle", true).containsEntry("secret", true).containsEntry("pivot", true);

        assertThat(v.retirerOperation("A").getCode()).isEqualTo("A");
        assertThat(v.operation("C").orElseThrow().predecesseursListe()).containsExactly("B");
        code(() -> v.retirerOperation("A"), "HOM_OPERATION_NOT_FOUND");
    }

    @Test
    @DisplayName("Arcs, positions, pilote ; version figée : HOM_REFERENTIAL_FROZEN pour toute écriture")
    void arcsPositionsPilote() {
        ReferentialVersion v = brouillon();
        v.creerOperation("A", simple());
        v.creerOperation("B", simple());
        assertThat(v.arc("A", "B", true).predecesseursListe()).containsExactly("A");
        assertThat(v.arc("A", "B", true).predecesseursListe()).containsExactly("A");
        assertThat(v.arc("A", "B", false).predecesseursListe()).isEmpty();
        code(() -> v.arc("A", "A", true), "HOM_OPERATION_SELF_PREDECESSOR");
        code(() -> v.arc("X", "A", true), "HOM_OPERATION_NOT_FOUND");
        v.placer(Map.of("A", new Position(1, 2)));
        assertThat(v.operation("A").orElseThrow().getPosition()).isEqualTo(new Position(1, 2));
        UUID pilote = UUID.randomUUID();
        v.designerPilote(pilote);
        assertThat(v.getPiloteId()).isEqualTo(pilote);
        ReferentialVersion derive = ReferentialVersion.deriverDe(v, "Dérivée", Instant.EPOCH);
        assertThat(derive.getPiloteId()).isNull();
        assertThat(derive.operation("A").orElseThrow().getPosition()).isEqualTo(new Position(1, 2));
        assertThat(derive.operation("A").orElseThrow().getParticularite()).isEqualTo(Particularite.FIN_ESSAIS);

        v.publier(1, Instant.EPOCH, List.of("A", "B"));
        code(() -> v.creerOperation("C", simple()), "HOM_REFERENTIAL_FROZEN");
        code(() -> v.retirerOperation("A"), "HOM_REFERENTIAL_FROZEN");
        code(() -> v.arc("A", "B", true), "HOM_REFERENTIAL_FROZEN");
        code(() -> v.placer(Map.of()), "HOM_REFERENTIAL_FROZEN");
        code(() -> v.designerPilote(pilote), "HOM_REFERENTIAL_FROZEN");
    }

    @Test
    @DisplayName("Saisie sans position ni particularité : celles déjà enregistrées sont conservées")
    void conservationPositionEtParticularite() {
        ReferentialVersion v = brouillon();
        v.creerOperation("A", simple());
        ReferenceOperation a = v.upsertOperation("A", simple().avecQualites(Fonction.SNA, Fonction.CONDUCTEUR));
        assertThat(a.getPosition()).isEqualTo(new Position(10, 20));
        ReferenceOperationData sans = new ReferenceOperationData("L", null, Segment.BRANCHE, null, null, null, null, null, List.of(), null, null);
        a = v.upsertOperation("A", sans);
        assertThat(a.getPosition()).isEqualTo(new Position(10, 20));
        assertThat(a.getParticularite()).isEqualTo(Particularite.FIN_ESSAIS);
        assertThat(a.getResponsable()).isNull();
    }

    @Test
    @DisplayName("Version antérieure au modèle : étape, R, V, I, geste, grain, plateforme et attributs lus de la saisie historique")
    void valeursEffectives() {
        ReferentialVersion v = brouillon();
        ReferenceOperation h = v.upsertOperation(
            "H",
            new ReferenceOperationData(
                "Historique",
                "MISE_EN_PRODUCTION",
                Segment.BRANCHE,
                Partie.PARTICIPANT,
                "TRANSVERSE",
                NatureCouverture.AUCUNE,
                Map.of("grain", "PHASE", "plateforme", "SWITCH_DELEGATAIRE", "cle", true, "secret", true, "valeur_reference", true, "pivot", true),
                null,
                List.of(),
                "DIRECTION",
                null
            )
        );
        assertThat(h.etapeEffective()).isEqualTo(EtapeCode.MISE_EN_PRODUCTION);
        assertThat(h.segmentEffectif()).isEqualTo(SegmentParcours.BRANCHE);
        assertThat(h.responsableEffectif()).isEqualTo(Fonction.PARTICIPANT);
        assertThat(h.validateurEffectif()).isEqualTo(Fonction.CONDUCTEUR);
        assertThat(h.informesEffectifs()).containsExactly(Fonction.CHEF_DDRM);
        assertThat(h.grainEffectif()).isEqualTo(GrainPreuve.PHASE);
        assertThat(h.gesteEffectif()).isEqualTo(Geste.DEPOSER_LIVRABLE);
        assertThat(h.plateformeEffective()).isEqualTo(PlateformeExecution.SWITCH_DELEGATAIRE);
        assertThat(h.cleEffective()).isTrue();
        assertThat(h.secretEffectif()).isTrue();
        assertThat(h.pivotEffectif()).isTrue();
        assertThat(h.valeurReferenceAttendue()).isTrue();
        assertThat(h.delaiEffectif()).isEqualTo(3);
        assertThat(h.ordreEffectif()).isEqualTo(Integer.MAX_VALUE);

        ReferenceOperation libre = v.upsertOperation(
            "L",
            new ReferenceOperationData("Libre", "Adhésion", null, Partie.DES, null, null, Map.of("plateforme", "INCONNUE"), null, List.of(), "CONDUCTEUR", null)
        );
        assertThat(libre.etapeEffective()).isEqualTo(EtapeCode.CADRAGE);
        assertThat(libre.segmentEffectif()).isEqualTo(SegmentParcours.AMONT);
        assertThat(libre.responsableEffectif()).isEqualTo(Fonction.CONDUCTEUR);
        assertThat(libre.validateurEffectif()).isEqualTo(Fonction.CHEF_DES);
        assertThat(libre.plateformeEffective()).isNull();
        assertThat(libre.gesteEffectif()).isEqualTo(Geste.DEPOSER_PREUVE);
        ReferenceOperation aval = v.upsertOperation("V", new ReferenceOperationData("Aval", null, Segment.TRONC_COMMUN_AVAL, Partie.DES, null, null, null, null, List.of(), null, null));
        assertThat(aval.etapeEffective()).isEqualTo(EtapeCode.MISE_EN_PRODUCTION);
        assertThat(aval.responsableEffectif()).isEqualTo(Fonction.CONDUCTEUR);
        assertThat(aval.noeud().modele().responsable()).isNull();
        assertThat(Segment.de("AMONT")).isEqualTo(Segment.TRONC_COMMUN_AMONT);
        assertThat(Segment.de("TRONC_COMMUN_AVAL")).isEqualTo(Segment.TRONC_COMMUN_AVAL);
        assertThat(Segment.de("BRANCHE")).isEqualTo(Segment.BRANCHE);
        assertThat(Segment.de(" ")).isNull();
        assertThatThrownBy(() -> Segment.de("AILLEURS")).isInstanceOf(IllegalArgumentException.class);
        assertThat(Segment.du(SegmentParcours.AVAL)).isEqualTo(Segment.TRONC_COMMUN_AVAL);
    }

    @Test
    @DisplayName("Contrôles du modèle (§2) : R et V, entrée, déclencheur, jalons uniques et ordonnés, secret, livrables")
    void controlesDuModele() {
        Livrable l = new Livrable("L1", "Pièce", NatureLivrable.LIVRABLE, Fonction.DDRM, List.of("application/pdf"), true);
        ReferentialVersion v = brouillon();
        v.upsertOperation("A", op(null, Fonction.CONDUCTEUR, Geste.EVENEMENT_EXTERNE, Jalon.EN_PRODUCTION, null, List.of(l, l), "C"));
        v.upsertOperation("B", op(Fonction.DDRM, Fonction.DDRM, Geste.DEPOSER_PREUVE, Jalon.CERTIFIEE, "certification.inconnu.v1", List.of(), "A"));
        v.upsertOperation("C", op(Fonction.DDRM, null, Geste.DEPOSER_PREUVE, Jalon.CERTIFIEE, Declencheurs.PV_SIGNE, List.of(), "B"));
        ReferentialGraphValidator.Rapport cyclique = new ReferentialGraphValidator().controler(v.noeuds());
        assertThat(cyclique.anomalies().get(0).type()).isEqualTo(TypeAnomalie.CYCLE);
        assertThat(cyclique.anomalies())
            .extracting(ReferentialGraphValidator.Anomalie::type)
            .contains(
                TypeAnomalie.RESPONSABLE_ABSENT,
                TypeAnomalie.VALIDATEUR_EGAL_RESPONSABLE,
                TypeAnomalie.VALIDATEUR_ABSENT,
                TypeAnomalie.DECLENCHEUR_INCONNU,
                TypeAnomalie.LIVRABLE_DUPLIQUE,
                TypeAnomalie.JALON_DUPLIQUE,
                TypeAnomalie.SANS_ENTREE
            )
            .doesNotContain(TypeAnomalie.JALON_DESORDONNE);

        ReferentialVersion w = brouillon();
        w.upsertOperation("A", op(Fonction.DDRM, Fonction.CONDUCTEUR, Geste.DEPOSER_PREUVE, Jalon.EN_PRODUCTION, null, List.of()));
        w.upsertOperation("B", op(Fonction.DDRM, Fonction.CONDUCTEUR, Geste.DEPOSER_PREUVE, Jalon.CERTIFIEE, null, List.of(), "A"));
        w.upsertOperation("C", op(Fonction.DDRM, Fonction.CONDUCTEUR, Geste.EVENEMENT_EXTERNE, null, Declencheurs.SECURITE_APPROUVEE, List.of(), "B"));
        ReferentialGraphValidator.Rapport r = new ReferentialGraphValidator().controler(w.noeuds());
        assertThat(r.anomalies()).singleElement().satisfies(a -> {
            assertThat(a.type()).isEqualTo(TypeAnomalie.JALON_DESORDONNE);
            assertThat(a.codes()).containsExactly("A", "B");
        });
        // Secret avec valeur relevée (écrit hors du modélisateur) : signalé.
        ReferenceOperation c = w.operation("C").orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.setField(c, "valeurReference", "Code");
        assertThat(new ReferentialGraphValidator().controler(w.noeuds()).anomalies()).extracting(ReferentialGraphValidator.Anomalie::type).contains(TypeAnomalie.SECRET_AVEC_VALEUR);
    }
}
