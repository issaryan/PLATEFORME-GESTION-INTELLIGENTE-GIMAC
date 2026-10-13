package cm.gimac.des.pilotage.domain.processus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** États d'une étape d'instance (D-87 §2) : activation, fin, signatures dans l'ordre, validation, renvoi, départ. */
class EtapeInstanceTest {

    private static final Instant LE = Instant.parse("2026-10-06T08:00:00Z");
    private static final Intervenant CHEF = new Intervenant(ModeIntervenant.CHEF_UNITE, null, null, null, "ROLE_RESPONSABLE_SERVICE");

    private static Etape definition(List<SignatureRequise> signatures) {
        return new Etape(
            "E",
            "Étape",
            null,
            CHEF,
            null,
            List.of(),
            2,
            List.of(new Livrable("L", "Livrable", List.of(), true, null), new Livrable("F", "Facultatif", List.of(), false, null)),
            signatures,
            List.of(new Champ("C", "Champ", TypeChamp.TEXTE, List.of(), true)),
            null
        );
    }

    private static EtapeInstance active(Etape def, Affectation validateur) {
        EtapeInstance e = new EtapeInstance(UUID.randomUUID(), def.code(), 0);
        List<SignatureInstance> s = def.signatures().stream().map(x -> SignatureInstance.attendue(x, Affectation.personne("chef", "U"))).toList();
        e.activer(UUID.randomUUID(), validateur, s, LocalDate.of(2026, 10, 8), LE);
        return e;
    }

    private static void remplir(EtapeInstance e) {
        e.deposer(new Depot("L", "d1", "l.pdf", "application/pdf", 10, "a".repeat(64), "x", LE.toString()));
        e.renseigner(Map.of("C", "valeur"));
    }

    @Test
    @DisplayName("D-106 : délai maximal effectif, échéance proche à la cible, retard au-delà du maximum")
    void delaiCibleEtMaximal() {
        Etape def = definition(List.of());
        assertThat(def.delaiMaxJours()).isNull();
        assertThat(def.delaiMaxEffectif()).isEqualTo(2);
        Etape avecMax = new Etape("E", "Étape", null, CHEF, null, List.of(), 2, List.of(), List.of(), List.of(), null, 6);
        assertThat(avecMax.delaiMaxEffectif()).isEqualTo(6);
        EtapeInstance e = new EtapeInstance(UUID.randomUUID(), "E", 0);
        LocalDate cible = LocalDate.of(2026, 10, 8);
        LocalDate max = LocalDate.of(2026, 10, 12);
        e.activer(UUID.randomUUID(), null, List.of(), cible, max, LE);
        assertThat(e.getEcheanceCible()).isEqualTo(cible);
        assertThat(e.getEcheance()).isEqualTo(max);
        assertThat(e.aLApproche(cible.minusDays(1))).isFalse();
        assertThat(e.aLApproche(cible)).isTrue();
        assertThat(e.aLApproche(max)).isTrue();
        assertThat(e.enRetard(max)).isFalse();
        assertThat(e.enRetard(max.plusDays(1))).isTrue();
        assertThat(e.aLApproche(max.plusDays(1))).isFalse();
        // Étape activée avant D-106 : une seule échéance, cible et maximum confondus.
        EtapeInstance ancienne = active(def, null);
        assertThat(ancienne.getEcheanceCible()).isEqualTo(ancienne.getEcheance());
    }

    @Test
    @DisplayName("Fin : livrables obligatoires et champs exigés, puis terminée sans signature ni validateur")
    void fin() {
        Etape def = definition(List.of());
        EtapeInstance e = active(def, null);
        assertThat(e.getEtat()).isEqualTo(EtatEtape.ACTIVE);
        assertThatThrownBy(() -> e.soumettre(def, LE)).isInstanceOfSatisfying(PilotageException.class, x -> assertThat(x.code()).isEqualTo("PIL_PROCESSUS_LIVRABLES_MANQUANTS"));
        e.deposer(new Depot("L", "d1", "l.pdf", "application/pdf", 10, "a".repeat(64), "x", LE.toString()));
        assertThatThrownBy(() -> e.soumettre(def, LE)).isInstanceOfSatisfying(PilotageException.class, x -> assertThat(x.code()).isEqualTo("PIL_PROCESSUS_CHAMPS_MANQUANTS"));
        e.renseigner(Map.of("C", "v"));
        e.deposer(new Depot("L", "d2", "l2.pdf", "application/pdf", 12, "b".repeat(64), "x", LE.toString()));
        assertThat(e.getDepots()).hasSize(1);
        assertThat(e.depot("L").documentId()).isEqualTo("d2");
        e.soumettre(def, LE);
        assertThat(e.getEtat()).isEqualTo(EtatEtape.TERMINEE);
        assertThatThrownBy(() -> e.renseigner(Map.of("C", "w"))).isInstanceOfSatisfying(PilotageException.class, x -> assertThat(x.code()).isEqualTo("PIL_PROCESSUS_ETAPE_ETAT"));
    }

    @Test
    @DisplayName("Signatures par ordre croissant, puis validation ; renvoi : étape active, signatures à refaire")
    void signaturesEtValidation() {
        SignatureRequise s1 = new SignatureRequise("S1", "Premier", CHEF, List.of("L"), 1);
        SignatureRequise s2 = new SignatureRequise("S2", "Second", CHEF, List.of("L"), 2);
        Etape def = definition(List.of(s1, s2));
        EtapeInstance e = active(def, Affectation.profil("ROLE_V", "U"));
        remplir(e);
        e.soumettre(def, LE);
        assertThat(e.getEtat()).isEqualTo(EtatEtape.A_SIGNER);
        assertThat(e.signaturesOuvertes()).extracting(SignatureInstance::code).containsExactly("S1");
        assertThatThrownBy(() -> e.signer("S2", "chef", "e", List.of(), LE)).isInstanceOfSatisfying(PilotageException.class, x -> assertThat(x.code()).isEqualTo("PIL_PROCESSUS_SIGNATURE_ORDRE"));
        e.signer("S1", "chef", "e1", List.of(), LE);
        assertThatThrownBy(() -> e.signer("S1", "chef", "e", List.of(), LE)).isInstanceOfSatisfying(PilotageException.class, x -> assertThat(x.code()).isEqualTo("PIL_PROCESSUS_DEJA_SIGNEE"));
        e.signer("S2", "chef", "e2", List.of(), LE);
        assertThat(e.getEtat()).isEqualTo(EtatEtape.A_VALIDER);
        e.renvoyer();
        assertThat(e.getEtat()).isEqualTo(EtatEtape.ACTIVE);
        assertThat(e.getRenvois()).isEqualTo(1);
        assertThat(e.getSignatures()).allMatch(s -> s.etat() == EtatSignature.ATTENDUE && s.signePar() == null);
        e.soumettre(def, LE);
        e.signer("S1", "chef", "e1", List.of(), LE);
        e.signer("S2", "chef", "e2", List.of(), LE);
        e.valider(LE);
        assertThat(e.getEtat()).isEqualTo(EtatEtape.TERMINEE);
        assertThat(e.getTermineeLe()).isEqualTo(LE);
    }

    @Test
    @DisplayName("Départ : signatures attendues de la personne reprises par profil ; validateur réaffecté ; annulation")
    void depart() {
        SignatureRequise s1 = new SignatureRequise("S1", "Premier", CHEF, List.of("L"), 1);
        Etape def = definition(List.of(s1));
        EtapeInstance e = active(def, Affectation.personne("chef", "U"));
        assertThat(e.getValidateurId()).isEqualTo("chef");
        List<String> codes = e.reaffecterSignatures("chef", s -> s.reaffectee(Affectation.profil("ROLE_R", "U")));
        assertThat(codes).containsExactly("S1");
        assertThat(e.signature("S1").attendu()).isEqualTo(Affectation.profil("ROLE_R", "U"));
        assertThat(e.reaffecterSignatures("autre", s -> s)).isEmpty();
        e.affecterValidateur(Affectation.profil("ROLE_R", "U"));
        assertThat(e.getValidateurId()).isNull();
        e.annuler(LE);
        assertThat(e.getEtat()).isEqualTo(EtatEtape.ANNULEE);
    }

    @Test
    @DisplayName("Instance : suspension, reprise, annulation, clôture")
    void instance() {
        Processus p = Processus.brouillon("P", "U", "a", LE);
        p.decrire("P", null, null, null, List.of(definition(List.of())), "a", LE);
        assertThatThrownBy(() -> p.retirer("a", "m", LE)).isInstanceOf(PilotageException.class);
        p.publier("a", null, false, LE);
        assertThatThrownBy(() -> p.decrire("P", null, null, null, List.of(), "a", LE)).isInstanceOfSatisfying(PilotageException.class, x -> assertThat(x.code()).isEqualTo("PIL_PROCESSUS_ETAT"));
        Processus v2 = p.nouvelleVersion(2, "a", LE);
        assertThat(v2.getVersion()).isEqualTo(2);
        assertThat(v2.getEtat()).isEqualTo(EtatProcessus.BROUILLON);
        assertThat(v2.getEtapes()).isEqualTo(p.getEtapes());
        InstanceProcessus i = new InstanceProcessus(p, "Essai", "l", null, Map.of(), LE);
        i.suspendre("m", LE);
        assertThatThrownBy(i::exigerEnCours).isInstanceOfSatisfying(PilotageException.class, x -> assertThat(x.code()).isEqualTo("PIL_PROCESSUS_INSTANCE_ETAT"));
        i.reprendre(null, LE);
        i.annuler("m", LE);
        assertThat(i.getEtat()).isEqualTo(EtatInstance.ANNULEE);
        assertThatThrownBy(() -> i.annuler("m", LE)).isInstanceOf(PilotageException.class);
    }

    @Test
    @DisplayName("D-93 : signataire attendu remplacé (prise de la tâche, organisation) ; une signature apposée ne change plus")
    void reaffecterSignature() {
        Etape def = definition(List.of(new SignatureRequise("S1", "Visa", CHEF, List.of("L"), 1), new SignatureRequise("S2", "Visa 2", CHEF, List.of("L"), 2)));
        EtapeInstance e = active(def, null);
        remplir(e);
        e.soumettre(def, LE);
        e.reaffecterSignature("S1", Affectation.personne("preneur", "U"));
        assertThat(e.signature("S1").attendu().personneId()).isEqualTo("preneur");
        e.signer("S1", "preneur", "b".repeat(64), List.of(), LE);
        e.reaffecterSignature("S1", Affectation.profil("ROLE_X", "U"));
        assertThat(e.signature("S1").attendu().personneId()).isEqualTo("preneur");
        e.reaffecterSignature("S2", Affectation.profil("ROLE_X", "U"));
        assertThat(e.signature("S2").attendu().profil()).isEqualTo("ROLE_X");
        assertThat(e.signaturesOuvertes()).extracting(SignatureInstance::code).containsExactly("S2");
    }
}
