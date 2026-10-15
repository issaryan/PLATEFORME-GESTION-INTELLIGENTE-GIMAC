package cm.gimac.des.homologation.domain.parcours;

import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.avancer;
import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.parcours;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.Vues.EtatLivrable;
import cm.gimac.des.homologation.domain.service.EtatsPreuves;
import cm.gimac.des.homologation.domain.service.PreuvesSignaturesCrV17;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exigence des preuves (D-109) : une pièce par service pour un livrable au grain du service, livrable sans objet motivé,
 * opération reprise jamais bloquée, pièce d'un geste dédié ou d'un événement inscrite, états des livrables et des
 * signatures, chronologie réelle.
 */
class PreuvesParcoursTest {

    static final LocalDate J = LocalDate.parse("2026-10-15");
    static final String SHA = "b".repeat(64);

    static void refus(ThrowingCallable c, String code) {
        assertThatThrownBy(c).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    static Preuve piece(String ref) {
        return Preuve.piece(ref, ref + ".pdf", SHA, J, "Agent");
    }

    /** Modèle D-109 du rang appliqué à l'opération du parcours (version de l'annexe A v1.7 complétée). */
    static OperationParcours d109(Parcours p, TypeProjet type, String rang) {
        ReferentialVersion v = ReferentialVersion.nouveauBrouillon(type, "D-109", Instant.parse("2026-10-15T00:00:00Z"));
        PreuvesSignaturesCrV17.version(type).forEach(o -> v.upsertOperation(o.code(), o.data()));
        ReferenceOperation ref = v.operation(rang).orElseThrow();
        OperationParcours o = p.operation(rang).orElseThrow();
        o.appliquerModele(ref);
        return o;
    }

    @Test
    @DisplayName("Grain du service (rang 16) : service exigé et inclus, une pièce par service avant la validation")
    void grainService() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT", "TRANSFERT");
        avancer(p, "15", J);
        OperationParcours o = d109(p, TypeProjet.CARTE_INTERBANCAIRE, "16");
        Livrable l = Parcours.livrableVise(o, null);
        assertThat(l.parService()).isTrue();
        assertThat(EtatsPreuves.etat(o, l, p.codesServices())).isEqualTo(EtatLivrable.ATTENDUE);
        refus(() -> p.deposerPreuve(o, l, null, piece("X"), null, null, null), "HOM_LIVRABLE_SERVICE_REQUIS");
        refus(() -> p.deposerPreuve(o, l, "INCONNU", piece("X"), null, null, null), "HOM_LIVRABLE_SERVICE_INCONNU");
        p.deposerPreuve(o, l, "RETRAIT", piece("R1"), null, null, null);
        assertThat(o.getEtat()).isEqualTo(EtatOperationParcours.EN_COURS);
        assertThat(o.servicesManquants(l, p.codesServices())).containsExactly("TRANSFERT");
        assertThatThrownBy(() -> Parcours.exigerLivrables(o, p.codesServices()))
            .isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.details()).containsEntry("services", Map.of("L1", List.of("TRANSFERT"))));
        assertThat(EtatsPreuves.services(o, l, p.codesServices())).extracting(Vues.ServiceLivrable::etat).containsExactly(EtatLivrable.DEPOSEE, EtatLivrable.ATTENDUE);
        p.deposerPreuve(o, l, "RETRAIT", piece("R2"), null, null, null);
        p.deposerPreuve(o, l, " TRANSFERT ", piece("T1"), null, null, null);
        assertThat(o.getDepots()).extracting(DepotLivrable::documentRef).containsExactly("R2", "T1");
        assertThat(o.getEtat()).isEqualTo(EtatOperationParcours.EN_VALIDATION);
        assertThat(EtatsPreuves.etat(o, l, p.codesServices())).isEqualTo(EtatLivrable.DEPOSEE);
        // Sans services connus : une pièce suffit ; un livrable de l'opération ignore le service.
        assertThat(o.livrablesManquants()).isEmpty();
        assertThat(p.serviceVise(new Livrable("L9", "x", NatureLivrable.PREUVE, Fonction.SNA, List.of(), true), "RETRAIT")).isNull();
        p.valider(o, new Acteur(UUID.randomUUID(), "V", java.util.Set.of(), null), J);
        assertThat(o.faite()).isTrue();
        assertThat(EtatsPreuves.etat(o, l, p.codesServices())).isEqualTo(EtatLivrable.VALIDEE);
    }

    @Test
    @DisplayName("Livrable sans objet (rang 4, sans banque partenaire) : motif exigé, tracé, l'opération passe en validation")
    void sansObjet() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        avancer(p, "3", J);
        OperationParcours o = d109(p, TypeProjet.CARTE_INTERBANCAIRE, "4");
        Livrable l = o.getLivrables().get(0);
        refus(() -> p.declarerSansObjet(o, l, null, " a ", "DDRM", "u1", J), "HOM_MOTIF_REQUIS");
        p.declarerSansObjet(o, l, null, "Participant non porté", "Chargé DDRM", "u1", J);
        assertThat(o.depot("L1")).hasValueSatisfying(d -> {
            assertThat(d.estSansObjet()).isTrue();
            assertThat(d.motif()).isEqualTo("Participant non porté");
            assertThat(d.deposePar()).isEqualTo("Chargé DDRM");
        });
        assertThat(o.getEtat()).isEqualTo(EtatOperationParcours.EN_VALIDATION);
        assertThat(EtatsPreuves.etat(o, l, p.codesServices())).isEqualTo(EtatLivrable.SANS_OBJET);
        p.valider(o, new Acteur(UUID.randomUUID(), "V", java.util.Set.of(), null), J);
        assertThat(o.faite()).isTrue();
        assertThat(EtatsPreuves.etat(o, l, p.codesServices())).isEqualTo(EtatLivrable.SANS_OBJET);
        refus(() -> p.declarerSansObjet(o, l, null, "Trop tard", "x", "u1", J), "HOM_OPERATION_NOT_OPEN");
    }

    @Test
    @DisplayName("Livrable au grain du service déclaré sans objet pour un service : les autres services restent attendus")
    void sansObjetParService() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT", "TRANSFERT");
        avancer(p, "29", J);
        OperationParcours o = d109(p, TypeProjet.CARTE_INTERBANCAIRE, "30");
        Livrable l = o.getLivrables().get(0);
        p.declarerSansObjet(o, l, "TRANSFERT", "Service non ouvert en production", "PM", "u2", J);
        assertThat(o.livrablesManquants(p.codesServices())).extracting(Livrable::code).containsExactly("L1");
        assertThat(EtatsPreuves.services(o, l, p.codesServices())).extracting(Vues.ServiceLivrable::etat).containsExactly(EtatLivrable.ATTENDUE, EtatLivrable.SANS_OBJET);
        p.deposerPreuve(o, l, "RETRAIT", piece("R30"), null, null, null);
        assertThat(o.getEtat()).isEqualTo(EtatOperationParcours.EN_VALIDATION);
    }

    @Test
    @DisplayName("Opération reprise (D-107) : jamais bloquée, livrables « reprise, pièce non numérisée », signatures « reprise »")
    void reprise() {
        Parcours p = parcours(TypeProjet.PAIEMENT_MOBILE, "RETRAIT");
        OperationParcours o = d109(p, TypeProjet.PAIEMENT_MOBILE, "7");
        o.setPreuve(Preuve.reprise(null, null, J, "Reprise", null, "reprise, pièce non numérisée"));
        o.setEtat(EtatOperationParcours.FAITE);
        o.setFaiteLe(J);
        assertThat(o.reprise()).isTrue();
        assertThat(o.livrablesManquants(p.codesServices())).isEmpty();
        Parcours.exigerLivrables(o, p.codesServices());
        assertThat(o.getLivrables()).allSatisfy(l -> assertThat(EtatsPreuves.etat(o, l, p.codesServices())).isEqualTo(EtatLivrable.REPRISE_SANS_PIECE));
        assertThat(EtatsPreuves.etatSignature(o, false)).isEqualTo("REPRISE");
        assertThat(EtatsPreuves.etatSignature(o, true)).isEqualTo("SIGNEE");
        // Pièce automatique jamais inscrite sur une opération reprise.
        p.inscrireAutomatiques(o, piece("AUTO"));
        assertThat(o.getDepots()).isEmpty();
        assertThat(EtatsPreuves.chronologie(p)).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo("REPRISE");
            assertThat(e.detail()).isEqualTo("reprise, pièce non numérisée");
        });
    }

    @Test
    @DisplayName("Opération close avant l'exigence : livrable « non exigé », signature « non recueillie » ; ouverte : attendue")
    void anterieure() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        OperationParcours o = d109(p, TypeProjet.CARTE_INTERBANCAIRE, "7");
        Livrable l = o.getLivrables().get(0);
        assertThat(EtatsPreuves.etatSignature(o, false)).isEqualTo("ATTENDUE");
        o.setEtat(EtatOperationParcours.FAITE);
        assertThat(EtatsPreuves.etat(o, l, p.codesServices())).isEqualTo(EtatLivrable.NON_EXIGEE);
        assertThat(EtatsPreuves.etatSignature(o, false)).isEqualTo("NON_RECUEILLIE");
        o.setEtat(EtatOperationParcours.EN_SIGNATURE);
        assertThat(EtatsPreuves.etat(o, l, p.codesServices())).isEqualTo(EtatLivrable.NON_EXIGEE);
    }

    @Test
    @DisplayName("Renvoi après dépôt : pièce refusée, à reprendre ; signatures exigées après validation, dans l'ordre")
    void renvoiEtSignatures() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        avancer(p, "6", J);
        OperationParcours o = d109(p, TypeProjet.CARTE_INTERBANCAIRE, "7");
        Livrable l1 = Parcours.livrableVise(o, "L1");
        p.deposerLivrable(o, l1, null, piece("CR7"), "application/pdf", null, false, J);
        assertThat(o.getEtat()).isEqualTo(EtatOperationParcours.EN_SIGNATURE);
        assertThat(o.signaturesSuivantes()).extracting(SignatureOperation::signataire).containsExactly(Fonction.CHEF_DDRM);
        assertThat(EtatsPreuves.etat(o, l1, p.codesServices())).isEqualTo(EtatLivrable.VALIDEE);
        // Une autre opération renvoyée après son dépôt : sa pièce est refusée.
        OperationParcours o8 = d109(p, TypeProjet.CARTE_INTERBANCAIRE, "8");
        o8.setEtat(EtatOperationParcours.A_FAIRE);
        Livrable l = Parcours.livrableVise(o8, "L1");
        p.deposerLivrable(o8, l, null, piece("CR8"), null, null, false, J);
        p.deposerLivrable(o8, Parcours.livrableVise(o8, "L2"), null, piece("P8"), null, null, false, J);
        assertThat(o8.getEtat()).isEqualTo(EtatOperationParcours.EN_SIGNATURE);
        o8.setEtat(EtatOperationParcours.EN_COURS);
        o8.ajouterRenvoi(new Renvoi("Compte rendu incomplet", "v", "Validateur", J));
        assertThat(EtatsPreuves.etat(o8, l, p.codesServices())).isEqualTo(EtatLivrable.REFUSEE);
        assertThat(EtatsPreuves.chronologie(p)).extracting(Vues.EvenementChronologie::type).contains("DEPOT", "RENVOI", "FAITE");
    }

    @Test
    @DisplayName("Geste dédié ou événement : la pièce produite est inscrite pour les livrables obligatoires, avec son origine")
    void piecesAutomatiques() {
        Parcours p = parcours(TypeProjet.DELEGATAIRE, "RETRAIT");
        avancer(p, "17", J);
        OperationParcours o18 = d109(p, TypeProjet.DELEGATAIRE, "18");
        p.pvEtabli(piece("PV-1"), J);
        assertThat(o18.faite()).isTrue();
        assertThat(o18.depot("L1")).hasValueSatisfying(d -> {
            assertThat(d.documentRef()).isEqualTo("PV-1");
            assertThat(d.origine()).isEqualTo(Declencheurs.PV_ETABLI);
        });
        p.inscrireAutomatiques(o18, null);
        p.inscrireAutomatiques(o18, Preuve.piece(" ", "x", SHA, J, "x"));
        assertThat(EtatsPreuves.chronologie(p)).anySatisfy(e -> assertThat(e.type()).isEqualTo("PIECE_AUTOMATIQUE"));
        // Signature du procès-verbal (rang 19) reprise du circuit de MS-CERTIFICATION sous le code GIMAC.
        OperationParcours o19 = d109(p, TypeProjet.DELEGATAIRE, "19");
        p.recueillirSignaturesProcesVerbal(List.of(new SignatureRecueillie("GIMAC", null, "Chef du DES", null, Instant.parse("2026-10-15T10:00:00Z"), SHA, List.of(), Declencheurs.PV_SIGNE)), J);
        p.pvSigne(true, true, piece("PV-SIG"), J);
        assertThat(o19.faite()).isTrue();
        assertThat(o19.getSignaturesRecueillies()).extracting(SignatureRecueillie::code).containsExactly("GIMAC");
        assertThat(EtatsPreuves.chronologie(p)).anySatisfy(e -> assertThat(e.type()).isEqualTo("SIGNATURE"));
    }

    @Test
    @DisplayName("Chronologie : blocage déclaré et levé, du plus récent au plus ancien ; livrable sans objet")
    void chronologie() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        avancer(p, "3", J);
        BlocageParcours b = p.declarerBlocage("Pièce manquante", "Relancer", ParcoursSamples.designation(p.liaison()).pour(Fonction.DDRM), J.plusDays(5), UUID.randomUUID(), J);
        p.leverBlocage(b, "Reçue", J.plusDays(2));
        OperationParcours o = d109(p, TypeProjet.CARTE_INTERBANCAIRE, "4");
        p.declarerSansObjet(o, o.getLivrables().get(0), null, "Participant non porté", "DDRM", "u", J.plusDays(3));
        List<Vues.EvenementChronologie> c = EtatsPreuves.chronologie(p);
        assertThat(c.get(0).type()).isEqualTo("SANS_OBJET");
        assertThat(c).extracting(Vues.EvenementChronologie::type).contains("BLOCAGE", "LEVEE", "FAITE");
        assertThat(c.get(c.size() - 1).date()).isBeforeOrEqualTo(c.get(0).date());
    }
}
