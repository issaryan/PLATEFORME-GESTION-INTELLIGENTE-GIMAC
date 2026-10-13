package cm.gimac.des.homologation.domain.parcours;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.RepriseParcours.Avancement;
import cm.gimac.des.homologation.domain.parcours.RepriseParcours.Bilan;
import cm.gimac.des.homologation.domain.parcours.RepriseParcours.EtatRepris;
import cm.gimac.des.homologation.domain.parcours.RepriseParcours.OperationReprise;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Reprise de l'avancement d'un projet existant (D-107) : états posés, moteur recalculé, plateforme protégée. */
class RepriseParcoursTest {

    static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 13);
    static final UUID ACTEUR = UUID.randomUUID();

    static OperationReprise op(String rang, EtatRepris etat, String commentaire) {
        return new OperationReprise(rang, etat, null, LocalDate.of(2026, 5, 2), commentaire, null, null, null, null);
    }

    static Bilan appliquer(Parcours p, Avancement a, List<OperationReprise> ops) {
        return RepriseParcours.appliquer(p, a, ops, LocalDate.of(2025, 11, 30), null, "Suivi Excel", "Reprise", ACTEUR, AUJOURDHUI);
    }

    @Test
    @DisplayName("À qualifier : rien n'est posé, le moteur garde les premières opérations à faire")
    void aQualifier() {
        Parcours p = ParcoursSamples.parcours(TypeProjet.PAIEMENT_MOBILE, "PAIEMENT_MOBILE");
        Bilan b = appliquer(p, Avancement.A_QUALIFIER, List.of());
        assertThat(b.modifiees()).isZero();
        assertThat(p.etat().getStatut()).isEqualTo(StatutParcours.CADREE);
        assertThat(p.operations()).noneMatch(OperationParcours::faite);
    }

    @Test
    @DisplayName("En production : opérations jusqu'au jalon faites et « reprises », services en essai et certifiés, liaison close")
    void enProduction() {
        Parcours p = ParcoursSamples.parcours(TypeProjet.PAIEMENT_MOBILE, "PAIEMENT_MOBILE");
        LocalDate cible = LocalDate.of(2027, 1, 1);
        Bilan b = RepriseParcours.appliquer(p, Avancement.EN_PRODUCTION, List.of(), LocalDate.of(2025, 11, 30), cible, null, "Reprise", ACTEUR, AUJOURDHUI);
        assertThat(b.modifiees()).isPositive();
        assertThat(p.etat().getStatut()).isEqualTo(StatutParcours.EN_PRODUCTION);
        assertThat(p.liaison().getEtat()).isEqualTo(EtatLiaison.CLOSE);
        OperationParcours jalon = p.parJalon(Jalon.EN_PRODUCTION).orElseThrow();
        assertThat(jalon.getFaiteLe()).isEqualTo(LocalDate.of(2025, 11, 30));
        assertThat(jalon.getPreuve().estReprise()).isTrue();
        assertThat(jalon.getPreuve().commentaire()).contains("Repris à l'état final");
        assertThat(p.services()).allMatch(s -> s.getCertifieLe() != null);
        // Réappliquer ne change rien.
        assertThat(RepriseParcours.appliquer(p, Avancement.EN_PRODUCTION, List.of(), null, null, null, "Reprise", ACTEUR, AUJOURDHUI).modifiees()).isZero();
    }

    @Test
    @DisplayName("Détaillé : faite avec pièce, sans objet, en cours, à faire ; suivantes ouvertes ; incohérence signalée")
    void detaille() {
        Parcours p = ParcoursSamples.parcours(TypeProjet.PAIEMENT_MOBILE, "PAIEMENT_MOBILE");
        List<OperationParcours> ops = p.operations();
        String r0 = ops.get(0).getRang();
        String r1 = ops.get(1).getRang();
        String r2 = ops.get(2).getRang();
        OperationParcours tardive = ops.get(ops.size() - 3);
        Bilan b = appliquer(
            p,
            Avancement.DETAILLE,
            List.of(
                new OperationReprise(r0, EtatRepris.FAITE, null, null, " PV de lancement ", new ValeurReference("BIN", "123456"), "DOC-1", "a".repeat(64), null),
                op(r1, EtatRepris.SANS_OBJET, null),
                op(r2, EtatRepris.EN_COURS, "Ateliers en cours"),
                op(tardive.getRang(), EtatRepris.FAITE, null)
            )
        );
        assertThat(b.modifiees()).isEqualTo(4);
        OperationParcours o0 = p.operation(r0).orElseThrow();
        assertThat(o0.getFaiteLe()).isEqualTo(AUJOURDHUI);
        assertThat(o0.getPreuve().documentRef()).isEqualTo("DOC-1");
        assertThat(o0.getPreuve().valeurReference().valeur()).isEqualTo("123456");
        assertThat(o0.getPreuve().commentaire()).isEqualTo("Reprise de l'existant : PV de lancement");
        assertThat(p.operation(r1).orElseThrow().getEtat()).isEqualTo(EtatOperationParcours.SANS_OBJET);
        OperationParcours o2 = p.operation(r2).orElseThrow();
        assertThat(o2.getEtat()).isEqualTo(EtatOperationParcours.EN_COURS);
        assertThat(o2.getPreuve().commentaire()).contains("Ateliers en cours");
        assertThat(b.avertissements()).anyMatch(a -> a.startsWith(tardive.getRang() + " faite alors que"));
        // Rouverte par une nouvelle reprise : à faire, sans preuve.
        Bilan rouverte = appliquer(p, Avancement.DETAILLE, List.of(op(r2, EtatRepris.A_FAIRE, null), op(r0, EtatRepris.FAITE, null)));
        assertThat(rouverte.modifiees()).isEqualTo(1);
        assertThat(p.operation(r2).orElseThrow().getPreuve()).isNull();
    }

    @Test
    @DisplayName("Bloquée : un blocage daté de la liaison, opérations ouvertes bloquées ; réappliqué : aucun nouveau blocage")
    void bloquee() {
        Parcours p = ParcoursSamples.parcours(TypeProjet.PAIEMENT_MOBILE, "PAIEMENT_MOBILE");
        String r0 = p.operations().get(0).getRang();
        OperationReprise bloquee = new OperationReprise(r0, EtatRepris.BLOQUEE, LocalDate.of(2026, 9, 1), null, "Attente BEAC", null, null, null, LocalDate.of(2026, 11, 1));
        appliquer(p, Avancement.DETAILLE, List.of(bloquee));
        assertThat(p.bloquee()).isTrue();
        assertThat(p.blocages()).singleElement().satisfies(b -> {
            assertThat(b.getMotif()).contains(r0, "Attente BEAC");
            assertThat(b.getDepuis()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(b.getEcheanceLevee()).isEqualTo(LocalDate.of(2026, 11, 1));
            assertThat(b.getProchaineAction()).isEqualTo(RepriseParcours.PROCHAINE_ACTION);
        });
        assertThat(p.operation(r0).orElseThrow().getEtat()).isEqualTo(EtatOperationParcours.BLOQUEE);
        appliquer(p, Avancement.DETAILLE, List.of(bloquee));
        assertThat(p.blocages()).hasSize(1);
        // Sans conducteur désigné ni dates : responsable « à affecter », échéance au jour de la reprise.
        Parcours q = ParcoursSamples.parcours(TypeProjet.PAIEMENT_MOBILE, "PAIEMENT_MOBILE");
        q.retirerCompte(ParcoursSamples.CONDUCTEUR.toString());
        appliquer(q, Avancement.DETAILLE, List.of(op(q.operations().get(0).getRang(), EtatRepris.BLOQUEE, null)));
        assertThat(q.blocages()).singleElement().satisfies(b -> assertThat(b.getEcheanceLevee()).isEqualTo(AUJOURDHUI));
    }

    @Test
    @DisplayName("Refus : opération inconnue ; opération faite ou en signature sur la plateforme jamais réécrite")
    void refus() {
        Parcours p = ParcoursSamples.parcours(TypeProjet.PAIEMENT_MOBILE, "PAIEMENT_MOBILE");
        assertThatThrownBy(() -> appliquer(p, Avancement.DETAILLE, List.of(op("ZZ99", EtatRepris.FAITE, null))))
            .isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_REPRISE_OPERATION_INCONNUE"));
        OperationParcours o0 = p.operations().get(0);
        o0.setEtat(EtatOperationParcours.FAITE);
        o0.setPreuve(Preuve.piece("DOC", "pv.pdf", "a".repeat(64), AUJOURDHUI, "Agent"));
        assertThatThrownBy(() -> appliquer(p, Avancement.DETAILLE, List.of(op(o0.getRang(), EtatRepris.A_FAIRE, null))))
            .isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_REPRISE_OPERATION_TRAITEE"));
        o0.setPreuve(null);
        assertThatThrownBy(() -> appliquer(p, Avancement.DETAILLE, List.of(op(o0.getRang(), EtatRepris.SANS_OBJET, null)))).isInstanceOf(HomologationException.class);
        OperationParcours o1 = p.operations().get(1);
        o1.setEtat(EtatOperationParcours.EN_SIGNATURE);
        assertThatThrownBy(() -> appliquer(p, Avancement.DETAILLE, List.of(op(o1.getRang(), EtatRepris.FAITE, null)))).isInstanceOf(HomologationException.class);
    }

    @Test
    @DisplayName("Identité d'état par état repris")
    void identique() {
        OperationParcours o = ParcoursSamples.parcours(TypeProjet.PAIEMENT_MOBILE).operations().get(0);
        for (EtatRepris e : EtatRepris.values()) {
            o.setEtat(EtatOperationParcours.valueOf(e.name()));
            assertThat(RepriseParcours.identique(o, op("X", e, null))).isTrue();
            o.setEtat(EtatOperationParcours.VERROUILLEE);
            assertThat(RepriseParcours.identique(o, op("X", e, null))).isFalse();
        }
    }

    @Test
    @DisplayName("Preuve reprise : marquée, conservée par les projections, sérialisée")
    void preuve() {
        Preuve p = Preuve.reprise(null, null, AUJOURDHUI, "Reprise", null, "Repris");
        assertThat(p.estReprise()).isTrue();
        assertThat(p.avecValeurReference(new ValeurReference("BIN", "1")).estReprise()).isTrue();
        assertThat(p.avecCle("ABCD", null).commentaire()).isEqualTo("Repris");
        assertThat(p.deposeeParFonction("Conducteur").estReprise()).isTrue();
        assertThat(Preuve.piece("D", "n", "e", AUJOURDHUI, "x").estReprise()).isFalse();
        Preuve relue = cm.gimac.des.homologation.domain.JsonTexte.lire(cm.gimac.des.homologation.domain.JsonTexte.ecrire(p), Preuve.class);
        assertThat(relue).isEqualTo(p);
    }
}
