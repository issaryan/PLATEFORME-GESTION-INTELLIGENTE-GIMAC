package cm.gimac.des.homologation.domain.parcours;

import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.avancer;
import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.parcours;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Modèle de l'opération et moteur unique (D-77, docs/lot1/modelisateur-processus.md §1 et §3). */
class ModeleParcoursTest {

    static final LocalDate J = LocalDate.parse("2026-10-06");
    static final Preuve PIECE = Preuve.piece("DOC", "p.pdf", "a".repeat(64), J, "Test");

    /** Saisie du modèle : étape, R et V, délai, prédécesseurs, geste. */
    static ReferenceOperationData saisie(String libelle, EtapeCode etape, Segment segment, Fonction r, Fonction v, int delai, Geste geste, String... preds) {
        return new ReferenceOperationData(
            libelle,
            null,
            segment,
            null,
            null,
            null,
            null,
            null,
            List.of(preds),
            null,
            null,
            null,
            "Actions attendues",
            etape,
            null,
            r,
            v,
            List.of(),
            List.of(Fonction.CHEF_DDRM),
            List.of(Fonction.PARTICIPANT),
            delai,
            GrainPreuve.OPERATION,
            null,
            false,
            false,
            false,
            false,
            null,
            List.of(),
            geste,
            null,
            null,
            null,
            null
        );
    }

    @Nested
    class Listes {

        @Test
        @DisplayName("Gestes : action proposée, gestes dédiés, libellés ; jalons, particularités, étapes, plateformes, fonctions libellés")
        void libelles() {
            assertThat(Geste.CONSTATER_COUVERTURE.action()).isEqualTo("CONSTATER_COUVERTURE");
            assertThat(Geste.DEPOSER_LIVRABLE.action()).isEqualTo("DEPOSER_LIVRABLE");
            assertThat(Geste.COORDONNEES_TECHNIQUES.action()).isEqualTo("DEPOSER_PREUVE");
            assertThat(Arrays.stream(Geste.values()).filter(Geste::dedie)).containsExactly(Geste.AFFECTER_RESSOURCES, Geste.FICHE_SUIVI, Geste.EVENEMENT_EXTERNE, Geste.AUCUN);
            assertThat(Arrays.stream(Geste.values()).map(Geste::libelle)).doesNotContainNull().doesNotHaveDuplicates();
            assertThat(Arrays.stream(Jalon.values()).map(Jalon::libelle)).doesNotContainNull().doesNotHaveDuplicates();
            assertThat(Arrays.stream(Particularite.values()).map(Particularite::libelle)).doesNotContainNull().doesNotHaveDuplicates();
            assertThat(Arrays.stream(EtapeCode.values()).map(EtapeCode::libelle)).doesNotContainNull().doesNotHaveDuplicates();
            assertThat(Arrays.stream(EtapeCode.values()).filter(EtapeCode::horsParcours)).containsExactly(EtapeCode.PRISE_CONTACT, EtapeCode.ADHESION);
            assertThat(Arrays.stream(PlateformeExecution.values()).map(PlateformeExecution::libelle)).doesNotContainNull().doesNotHaveDuplicates();
            assertThat(Arrays.stream(Fonction.values()).map(Fonction::libelle)).doesNotContainNull().doesNotHaveDuplicates();
            assertThat(Fonction.SNA.libelle()).isEqualTo("Normes et Agréments");
            assertThat(Declencheurs.connu(Declencheurs.PV_SIGNE)).isTrue();
            assertThat(Declencheurs.connu("certification.inconnu.v1")).isFalse();
            assertThat(Declencheurs.connu(null)).isFalse();
            assertThat(Declencheurs.CONNUS).hasSize(6);
            assertThat(new Livrable("L", "x", NatureLivrable.PREUVE, Fonction.PARTICIPANT, null, false).typesPieces()).isEmpty();
            assertThat(new Position(1.5, 2)).isEqualTo(new Position(1.5, 2.0));
            assertThat(NatureLivrable.values()).hasSize(2);
        }
    }

    @Nested
    class Calendrier {

        @Test
        @DisplayName("Fin au plus tôt : maximum des prédécesseurs plus la durée ; chemin critique et durée totale ; prédécesseur inconnu ignoré")
        void auPlusTot() {
            Map<String, CalendrierDag.Tache> g = new LinkedHashMap<>();
            g.put("A", new CalendrierDag.Tache(List.of("Z"), 2));
            g.put("B", new CalendrierDag.Tache(List.of("A"), 5));
            g.put("C", new CalendrierDag.Tache(List.of("A"), 1));
            g.put("D", new CalendrierDag.Tache(List.of("B", "C"), 3));
            g.put("E", new CalendrierDag.Tache(List.of(), -4));
            CalendrierDag.Resultat r = CalendrierDag.calculer(g);
            assertThat(r.fins()).containsEntry("A", 2).containsEntry("B", 7).containsEntry("C", 3).containsEntry("D", 10).containsEntry("E", 0);
            assertThat(r.debuts()).containsEntry("D", 7);
            assertThat(r.cheminCritique()).containsExactly("A", "B", "D");
            assertThat(r.dureeTotale()).isEqualTo(10);
            assertThat(CalendrierDag.successeurs(Map.of("A", List.of(), "B", List.of("A", "A", "X"), "C", List.of("A")))).containsEntry("A", List.of("B", "C")).containsEntry("B", List.of());
        }

        @Test
        @DisplayName("Cycle : ses nœuds et leurs successeurs ne sont pas planifiés ; graphe vide : durée nulle")
        void cycle() {
            Map<String, CalendrierDag.Tache> g = new LinkedHashMap<>();
            g.put("A", new CalendrierDag.Tache(List.of("C"), 1));
            g.put("B", new CalendrierDag.Tache(List.of("A"), 1));
            g.put("C", new CalendrierDag.Tache(List.of("B"), 1));
            g.put("D", new CalendrierDag.Tache(List.of(), 2));
            CalendrierDag.Resultat r = CalendrierDag.calculer(g);
            assertThat(r.fins()).containsOnlyKeys("D");
            assertThat(r.cheminCritique()).containsExactly("D");
            assertThat(CalendrierDag.calculer(Map.of()).dureeTotale()).isZero();
        }
    }

    @Nested
    class Moteur {

        @Test
        @DisplayName("Échéances au plus tôt sur le graphe : opérations parallèles alignées, raccord de la branche, jalons et gestes lus")
        void echeancesEtLectures() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
            LocalDate d = ParcoursSamples.DEBUT;
            assertThat(p.operation("4").orElseThrow().getEcheance()).isEqualTo(d.plusDays(6));
            assertThat(p.operation("5").orElseThrow().getEcheance()).isEqualTo(d.plusDays(6));
            assertThat(p.operation("6").orElseThrow().getEcheance()).isEqualTo(d.plusDays(6));
            assertThat(p.operation("27").orElseThrow().getEcheance()).isEqualTo(p.operation("C10").orElseThrow().getEcheance().plusDays(3));
            assertThat(p.parGeste(Geste.AFFECTER_RESSOURCES).orElseThrow().getRang()).isEqualTo("6");
            assertThat(p.parJalon(Jalon.EN_PRODUCTION).orElseThrow().getRang()).isEqualTo("31");
            assertThat(p.parDeclencheur(Declencheurs.ATTESTATIONS_ARCHIVEES)).extracting(OperationParcours::getRang).containsExactly("21", "22");
            assertThat(p.premiereDeLEtape(EtapeCode.ATELIERS).orElseThrow().getRang()).isEqualTo("8");
            assertThat(p.premiereDuResponsable(Fonction.SNA).orElseThrow().getRang()).isEqualTo("10");
            OperationParcours o15 = p.operation("15").orElseThrow();
            assertThat(o15.getLibelle()).isEqualTo("Paramétrage d'essai");
            assertThat(o15.getParticularite()).isEqualTo(Particularite.ENTREE_ESSAIS);
            assertThat(o15.getLivrables()).hasSize(1);
            assertThat(o15.getDelaiJours()).isEqualTo(3);
            assertThat(o15.getResponsable()).isEqualTo(Fonction.SNA);
            assertThat(o15.getVersionId()).isEqualTo(p.liaison().getVersion().getId());
            assertThat(p.etat().getVersionId()).isNull();
            p.etat().rattacher(p.liaison().getVersion().getId(), 2);
            assertThat(p.etat().getVersionNumero()).isEqualTo(2);
            assertThat(p.jalonAtteint(Jalon.CERTIFIEE)).isFalse();
        }

        @Test
        @DisplayName("Point de passage (geste AUCUN) : clos dès qu'il devient éligible, y compris pendant un blocage")
        void pointDePassage() {
            ReferentialVersion v = ReferentialVersion.nouveauBrouillon(TypeProjet.DELEGATAIRE, "Essai", Instant.EPOCH);
            v.setId(UUID.randomUUID());
            v.upsertOperation("A", saisie("A", EtapeCode.CADRAGE, Segment.TRONC_COMMUN_AMONT, Fonction.DDRM, Fonction.CONDUCTEUR, 2, Geste.DEPOSER_PREUVE));
            v.upsertOperation("P", saisie("Passage", EtapeCode.CADRAGE, Segment.TRONC_COMMUN_AMONT, Fonction.DDRM, Fonction.CONDUCTEUR, 0, Geste.AUCUN, "A"));
            v.upsertOperation("B", saisie("B", EtapeCode.LANCEMENT, Segment.TRONC_COMMUN_AMONT, Fonction.SNA, Fonction.CONDUCTEUR, 4, Geste.DEPOSER_PREUVE, "P"));
            var l = ParcoursSamples.liaison(TypeProjet.DELEGATAIRE);
            ParcoursLiaison e = new ParcoursLiaison(l.getId(), "LIA-2026-009", ParcoursSamples.DEBUT);
            Parcours p = Parcours.instancier(l, e, v.getOperations(), List.of(), ParcoursSamples.designation(l));
            assertThat(p.operation("B").orElseThrow().getEcheance()).isEqualTo(ParcoursSamples.DEBUT.plusDays(6));
            assertThat(p.operation("B").orElseThrow().getRaci().e()).extracting(Personne::fonction).containsExactly(Fonction.PARTICIPANT);
            p.declarerBlocage("Attente", "Relance", new Personne("x", "x", Fonction.DDRM), J, UUID.randomUUID(), J);
            p.terminer(p.operation("A").orElseThrow(), PIECE, J);
            assertThat(p.operation("P").orElseThrow().getEtat()).isEqualTo(EtatOperationParcours.FAITE);
            assertThat(p.operation("P").orElseThrow().getFaiteLe()).isEqualTo(J);
            assertThat(p.operation("B").orElseThrow().getEtat()).isEqualTo(EtatOperationParcours.BLOQUEE);
            assertThat(Parcours.gesteDedie(p.operation("P").orElseThrow())).isTrue();
        }

        @Test
        @DisplayName("Migration : ajoutées verrouillées puis éligibles, retirées supprimées ou sans objet, modifiées mises à jour, faites intactes")
        void migration() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
            avancer(p, "3", J);
            OperationParcours o4 = p.operation("4").orElseThrow();
            p.deposerPreuve(o4, PIECE, null, null, null);
            Personne choisi = new Personne(UUID.randomUUID().toString(), "Chef DES choisi", Fonction.CHEF_DES);
            OperationParcours o6 = p.operation("6").orElseThrow();
            p.attribuer(o6, Qualite.R, choisi);
            UUID ancienne = o6.getVersionId();

            ReferentialVersion v2 = ReferentialVersion.deriverDe(p.liaison().getVersion(), "v2", Instant.EPOCH);
            v2.setId(UUID.randomUUID());
            v2.retirerOperation("5");
            v2.retirerOperation("4");
            v2.retirerOperation("3");
            ReferenceOperation r6 = v2.operation("6").orElseThrow();
            v2.upsertOperation("6", donnees(r6, 10, List.of()));
            ReferenceOperation r7 = v2.operation("7").orElseThrow();
            v2.upsertOperation("7", donnees(r7, 3, List.of("6", "X1")));
            v2.upsertOperation("X1", saisie("Nouvelle opération", EtapeCode.LANCEMENT, Segment.TRONC_COMMUN_AMONT, Fonction.SNA, Fonction.CONDUCTEUR, 2, Geste.DEPOSER_PREUVE));
            v2.operation("X1").orElseThrow().setVersion(v2);

            Parcours.Differentiel d = p.migrer(v2.getId(), 3, v2.getOperations(), ParcoursSamples.designation(p.liaison()), J);

            assertThat(d.supprimees()).extracting(OperationParcours::getRang).containsExactly("5");
            assertThat(d.sansObjet()).isEqualTo(1);
            assertThat(d.ajoutees()).extracting(OperationParcours::getRang).containsExactly("X1");
            assertThat(d.modifiees()).isEqualTo(2);
            assertThat(p.operation("5")).isEmpty();
            assertThat(p.operation("4").orElseThrow().getEtat()).isEqualTo(EtatOperationParcours.SANS_OBJET);
            assertThat(p.operation("3").orElseThrow().getEtat()).isEqualTo(EtatOperationParcours.FAITE);
            assertThat(p.operation("3").orElseThrow().getVersionId()).isEqualTo(v2.getId());
            assertThat(p.operation("X1").orElseThrow().getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
            assertThat(p.operation("X1").orElseThrow().getEcheance()).isEqualTo(ParcoursSamples.DEBUT.plusDays(2));
            assertThat(p.operation("6").orElseThrow().getEcheance()).isEqualTo(ParcoursSamples.DEBUT.plusDays(10));
            assertThat(p.operation("6").orElseThrow().getVersionId()).isEqualTo(v2.getId()).isNotEqualTo(ancienne);
            // Fonctions inchangées : la personne choisie reste en place.
            assertThat(p.operation("6").orElseThrow().getRaci().r()).isEqualTo(choisi);
            assertThat(p.operation("7").orElseThrow().getPredecesseurs()).containsExactly("6", "X1");
            assertThat(p.operation("7").orElseThrow().getEtat()).isEqualTo(EtatOperationParcours.VERROUILLEE);
            assertThat(p.etat().getVersionId()).isEqualTo(v2.getId());
            assertThat(p.etat().getVersionNumero()).isEqualTo(3);
            assertThat(p.operations().get(p.operations().size() - 1).getRang()).isEqualTo("4");
        }

        @Test
        @DisplayName("Migration : fonctions changées, qualités recalculées ; une opération éligible dont un prédécesseur est ajouté redevient verrouillée")
        void migrationQualites() {
            Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
            avancer(p, "3", J);
            assertThat(p.operation("4").orElseThrow().getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
            ReferentialVersion v2 = ReferentialVersion.deriverDe(p.liaison().getVersion(), "v2", Instant.EPOCH);
            v2.setId(UUID.randomUUID());
            ReferenceOperation r4 = v2.operation("4").orElseThrow();
            v2.upsertOperation("4", donnees(r4, 3, List.of("3", "Y")).avecQualites(Fonction.POLE_RESEAU, Fonction.SNA));
            v2.upsertOperation("Y", saisie("Y", EtapeCode.CADRAGE, Segment.TRONC_COMMUN_AMONT, Fonction.SNA, Fonction.CONDUCTEUR, 1, Geste.DEPOSER_PREUVE, "3"));
            v2.getOperations().forEach(o -> o.setVersion(v2));
            p.migrer(v2.getId(), 3, v2.getOperations(), ParcoursSamples.designation(p.liaison()), J);
            OperationParcours o4 = p.operation("4").orElseThrow();
            assertThat(o4.getRaci().r().fonction()).isEqualTo(Fonction.POLE_RESEAU);
            assertThat(o4.getRaci().v().fonction()).isEqualTo(Fonction.SNA);
            assertThat(o4.getEtat()).isEqualTo(EtatOperationParcours.VERROUILLEE);
            assertThat(p.operation("Y").orElseThrow().getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
        }
    }

    /** Saisie qui reproduit une opération de la version, délai et prédécesseurs remplacés. */
    static ReferenceOperationData donnees(ReferenceOperation o, int delai, List<String> preds) {
        return new ReferenceOperationData(
            o.getLibelle(),
            o.getPhase(),
            o.getSegment(),
            o.getPartie(),
            o.getServiceCode(),
            o.getNatureCouverture(),
            cm.gimac.des.homologation.domain.JsonTexte.objet(o.getPreuveAttendue()),
            o.getIdScenario(),
            preds,
            o.getFonctionResponsable(),
            null,
            o.getCode(),
            o.getDescription(),
            o.getEtape(),
            o.getOrdre(),
            o.getResponsable(),
            o.getValidateur(),
            o.getConsultes(),
            o.getInformes(),
            o.getExternes(),
            delai,
            o.getGrain(),
            o.getPlateforme(),
            o.isCle(),
            o.isSecret(),
            o.isOubliSansSymptome(),
            o.isPivot(),
            o.getValeurReference(),
            o.getLivrables(),
            o.getGeste(),
            o.getJalon(),
            o.getDeclencheur(),
            o.getPosition(),
            o.getParticularite()
        );
    }
}
