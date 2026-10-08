package cm.gimac.des.homologation.domain.parcours;

import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.avancer;
import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.parcours;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Effet réel des référentiels dans le parcours (D-93) : RACI imposé, livrables appliqués, renvoi, affectation, départ, vue. */
class EffetReelParcoursTest {

    static final LocalDate J = LocalDate.parse("2026-10-06");
    static final String SHA = "a".repeat(64);
    static final Acteur CONDUCTEUR = new Acteur(ParcoursSamples.CONDUCTEUR, "Conducteur de projet 1", Set.of(), null);
    static final Acteur SNA = new Acteur(ParcoursSamples.SNA, "Ingénieur SNA", Set.of("ROLE_SNA"), null);

    static void refus(ThrowingCallable c, String code) {
        assertThatThrownBy(c).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    /** Copie d'une saisie dont un composant change (les saisies sont des records). */
    static ReferenceOperationData avec(ReferenceOperationData d, String champ, Object valeur) throws Exception {
        RecordComponent[] c = ReferenceOperationData.class.getRecordComponents();
        Object[] args = new Object[c.length];
        Class<?>[] types = new Class<?>[c.length];
        for (int i = 0; i < c.length; i++) {
            types[i] = c[i].getType();
            args[i] = c[i].getName().equals(champ) ? valeur : c[i].getAccessor().invoke(d);
        }
        return ReferenceOperationData.class.getDeclaredConstructor(types).newInstance(args);
    }

    /** Opération 10 du parcours remodelée : deux livrables obligatoires (L1 par SNA, PDF ; L2 par le conducteur), consultés sollicités. */
    static OperationParcours deuxLivrables(Parcours p) throws Exception {
        OperationParcours o = p.operation("10").orElseThrow();
        ReferenceOperation ref = p.liaison().getVersion().operation("10").orElseThrow();
        ReferenceOperationData d = cm.gimac.des.homologation.domain.service.ConversionAnnexeA.version1(TypeProjet.CARTE_INTERBANCAIRE)
            .stream()
            .filter(x -> x.code().equals("10"))
            .findFirst()
            .orElseThrow()
            .data();
        d = avec(
            d,
            "livrables",
            List.of(
                new Livrable("L1", "Compte rendu", NatureLivrable.LIVRABLE, Fonction.SNA, List.of("application/pdf"), true),
                new Livrable("L2", "Annexe", NatureLivrable.LIVRABLE, Fonction.CONDUCTEUR, List.of(), true)
            )
        );
        d = avec(d, "consultesRecoiventTache", Boolean.TRUE);
        d = avec(d, "vue", VueOperation.ESSAIS);
        ref.appliquer(d);
        o.appliquerModele(ref);
        return o;
    }

    static Preuve piece(String ref) {
        return Preuve.piece(ref, ref + ".pdf", SHA, J, "Test");
    }

    @Test
    @DisplayName("Livrables du référentiel : code exigé parmi plusieurs, inconnu refusé, type contrôlé ; clôture au dernier obligatoire")
    void livrables() throws Exception {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        avancer(p, "9", J);
        OperationParcours o = deuxLivrables(p);
        assertThat(o.getVue()).isEqualTo(VueOperation.ESSAIS);
        assertThat(o.isConsultesRecoiventTache()).isTrue();
        assertThat(o.getEtapeCode()).isEqualTo("ATELIERS");
        refus(() -> Parcours.livrableVise(o, null), "HOM_LIVRABLE_REQUIS");
        refus(() -> Parcours.livrableVise(o, "L9"), "HOM_LIVRABLE_INCONNU");
        Livrable l1 = Parcours.livrableVise(o, "L1");
        Livrable l2 = Parcours.livrableVise(o, " L2 ");
        refus(() -> Parcours.verifierType(l1, "image/png"), "HOM_LIVRABLE_TYPE_REFUSE");
        Parcours.verifierType(l1, "application/pdf; charset=binary");
        Parcours.verifierType(l2, null);
        Parcours.verifierType(null, null);

        p.deposerLivrable(o, l1, piece("D1"), "application/pdf", ParcoursSamples.SNA.toString(), false, J);
        assertThat(o.getEtat()).isEqualTo(EtatOperationParcours.EN_COURS);
        assertThat(o.livrablesManquants()).extracting(Livrable::code).containsExactly("L2");
        refus(() -> Parcours.exigerLivrables(o), "HOM_LIVRABLE_MANQUANT");
        p.deposerLivrable(o, l1, piece("D1bis"), "application/pdf", null, true, J);
        assertThat(o.depot("L1")).map(DepotLivrable::documentRef).contains("D1bis");
        assertThat(o.getDepots()).hasSize(1).first().satisfies(d -> assertThat(d.supervision()).isTrue());
        p.deposerLivrable(o, l2, piece("D2"), null, ParcoursSamples.CONDUCTEUR.toString(), false, J);
        assertThat(o.faite()).isTrue();
        assertThat(o.getDepots()).extracting(DepotLivrable::livrable).containsExactly("L1", "L2");
    }

    @Test
    @DisplayName("RACI imposé : R désigné, porteur de la fonction à affecter, déposant, conducteur, supervision ; sinon refus")
    void droitDepot() throws Exception {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        avancer(p, "9", J);
        OperationParcours o = deuxLivrables(p);
        Livrable l1 = Parcours.livrableVise(o, "L1");
        Acteur inconnu = new Acteur(UUID.randomUUID(), "x", Set.of(), null);
        Set<Fonction> aucune = Set.of();
        assertThat(p.droitDepot(o, l1, SNA, Fonction.desProfils(SNA.permissions()))).contains(Parcours.DroitDepot.RESPONSABLE);
        assertThat(p.droitDepot(o, l1, CONDUCTEUR, aucune)).contains(Parcours.DroitDepot.CONDUCTEUR);
        assertThat(p.droitDepot(o, null, new Acteur(UUID.randomUUID(), "y", Set.of("HOM_STEP_MANAGE"), null), aucune)).contains(Parcours.DroitDepot.SUPERVISION);
        assertThat(p.droitDepot(o, null, new Acteur(null, "y", Set.of("HOM_STEP_MANAGE"), null), aucune)).isEmpty();
        refus(() -> p.exigerDroitDepot(o, l1, inconnu, aucune), "HOM_DEPOT_NON_AUTORISE");
        // R à affecter : tout porteur de sa fonction ; déposant d'un livrable sans désignation : porteur de sa fonction.
        o.setRaci(o.getRaci().avecR(Personne.aAffecter(Fonction.SNA)));
        Acteur porteur = new Acteur(UUID.randomUUID(), "Autre SNA", Set.of("ROLE_SNA"), null);
        assertThat(p.droitDepot(o, l1, porteur, Fonction.desProfils(porteur.permissions()))).contains(Parcours.DroitDepot.PORTEUR);
        o.setRaci(o.getRaci().avecR(new Personne(UUID.randomUUID().toString(), "R", Fonction.DDRM)));
        assertThat(p.droitDepot(o, l1, porteur, Fonction.desProfils(porteur.permissions()))).contains(Parcours.DroitDepot.DEPOSANT);
        refus(() -> p.exigerDroitDepot(o, l1, porteur, aucune), "HOM_DEPOT_NON_AUTORISE");
    }

    @Test
    @DisplayName("Validation et renvoi : V désigné (ou porteur de V à affecter), jamais R ; motif exigé ; l'opération revient à faire")
    void renvoi() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        OperationParcours o3 = p.operation("3").orElseThrow();
        p.deposerPreuve(o3, piece("P3"), null, null, null);
        assertThat(o3.getEtat()).isEqualTo(EtatOperationParcours.EN_VALIDATION);
        Set<Fonction> aucune = Set.of();
        assertThat(p.peutValider(o3, CONDUCTEUR, aucune)).isTrue();
        assertThat(p.peutValider(o3, SNA, aucune)).isFalse();
        refus(() -> p.exigerValidateur(o3, SNA, aucune), "HOM_VALIDATION_NON_VALIDATEUR");
        p.exigerValidateur(o3, CONDUCTEUR, aucune);
        refus(() -> p.renvoyer(o3, CONDUCTEUR, " ", J), "HOM_MOTIF_REQUIS");
        refus(() -> p.renvoyer(o3, CONDUCTEUR, "x".repeat(501), J), "HOM_MOTIF_REQUIS");
        Acteur r = new Acteur(UUID.randomUUID(), "R", Set.of(), null);
        o3.setRaci(o3.getRaci().avecR(new Personne(r.id().toString(), "R", Fonction.DDRM)));
        refus(() -> p.renvoyer(o3, r, "motif", J), "HOM_VALIDATOR_IS_RESPONSIBLE");
        Renvoi renvoi = p.renvoyer(o3, CONDUCTEUR, " Pièce illisible ", J);
        assertThat(renvoi).isEqualTo(new Renvoi("Pièce illisible", ParcoursSamples.CONDUCTEUR.toString(), "Conducteur de projet 1", J));
        assertThat(o3.getEtat()).isEqualTo(EtatOperationParcours.A_FAIRE);
        assertThat(o3.getRenvois()).containsExactly(renvoi);
        refus(() -> p.renvoyer(o3, CONDUCTEUR, "encore", J), "HOM_OPERATION_NOT_IN_VALIDATION");
        // V à affecter : un porteur de sa fonction valide.
        o3.setEtat(EtatOperationParcours.EN_VALIDATION);
        o3.setRaci(o3.getRaci().avecV(Personne.aAffecter(Fonction.CHEF_DES)));
        Acteur chef = new Acteur(UUID.randomUUID(), "Chef", Set.of("ROLE_CHEF_DES"), null);
        assertThat(p.peutValider(o3, chef, Fonction.desProfils(chef.permissions()))).isTrue();
        assertThat(p.peutValider(o3, new Acteur(null, null, Set.of(), null), aucune)).isFalse();
        p.declarerBlocage("m", "a", new Personne("u", "n", Fonction.DDRM), J, null, J);
        o3.setEtat(EtatOperationParcours.EN_VALIDATION);
        p.renvoyer(o3, chef, "bloquée", J);
        assertThat(o3.getEtat()).isEqualTo(EtatOperationParcours.BLOQUEE);
    }

    @Test
    @DisplayName("Affectation d'une fonction, institution liée, départ d'un compte (rôles à affecter, conducteur à désigner)")
    void affectationEtDepart() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        String sna = ParcoursSamples.SNA.toString();
        assertThat(p.implique(sna)).isTrue();
        assertThat(p.implique(UUID.randomUUID().toString())).isFalse();
        assertThat(p.retirerCompte(sna)).contains(Fonction.SNA);
        assertThat(p.operations()).filteredOn(o -> !o.faite()).allSatisfy(o -> assertThat(o.getRaci().toutes()).noneMatch(x -> sna.equals(x.id())));
        assertThat(p.tenant(Fonction.SNA)).isEmpty();
        Personne nouveau = new Personne(UUID.randomUUID().toString(), "Nouvel ingénieur", Fonction.SNA);
        p.affecterFonction(Fonction.SNA, nouveau);
        assertThat(p.tenant(Fonction.SNA)).contains(nouveau);
        assertThat(p.implique(nouveau.id())).isTrue();
        assertThat(p.retirerCompte(nouveau.id())).contains(Fonction.SNA);
        assertThat(p.tenant(Fonction.SNA)).isEmpty();

        UUID banque = UUID.randomUUID();
        Personne b = Personne.institution(Fonction.PERSONNALISATEUR, banque.toString(), null);
        assertThat(b.nom()).isEqualTo(Fonction.PERSONNALISATEUR.libelle());
        assertThat(b.institutionnelle()).isTrue();
        assertThat(b.institutionId()).isEqualTo(banque.toString());
        assertThat(b.parFonction()).isEqualTo(b);
        assertThat(nouveau.institutionId()).isNull();
        p.affecterFonction(Fonction.PERSONNALISATEUR, Personne.institution(Fonction.PERSONNALISATEUR, banque.toString(), "Perso SA"));
        assertThat(p.etat().getInstitutionsLiees()).containsKey(Fonction.PERSONNALISATEUR);
        assertThat(p.tenant(Fonction.PERSONNALISATEUR)).map(Personne::nom).contains("Perso SA");

        assertThat(p.conducteur()).isNotNull();
        assertThat(p.conduitePar(CONDUCTEUR)).isTrue();
        assertThat(p.retirerCompte(ParcoursSamples.CONDUCTEUR.toString())).contains(Fonction.CONDUCTEUR);
        assertThat(p.etat().isConducteurADesigner()).isTrue();
        assertThat(p.conducteur()).isNull();
        assertThat(p.conduitePar(CONDUCTEUR)).isFalse();
    }

    @Test
    @DisplayName("Vue d'une opération : correspondance des rangs de l'annexe A, déduction du modèle")
    void vues() {
        assertThat(VueOperation.duRang(null)).isEqualTo(VueOperation.AUCUNE);
        assertThat(List.of("6", "12", "16", "19", "21", "33", "c1", "7")).extracting(VueOperation::duRang).containsExactly(
            VueOperation.AFFECTATION,
            VueOperation.COORDONNEES,
            VueOperation.ESSAIS,
            VueOperation.DECISION,
            VueOperation.ATTESTATIONS,
            VueOperation.MISE_EN_SERVICE,
            VueOperation.CEREMONIE,
            VueOperation.AUCUNE
        );
        assertThat(VueOperation.deduite(Geste.AFFECTER_RESSOURCES, null, null)).isEqualTo(VueOperation.AFFECTATION);
        assertThat(VueOperation.deduite(Geste.COORDONNEES_TECHNIQUES, null, null)).isEqualTo(VueOperation.COORDONNEES);
        assertThat(VueOperation.deduite(Geste.DEPOSER_PREUVE, Particularite.FIN_ESSAIS, null)).isEqualTo(VueOperation.ESSAIS);
        assertThat(VueOperation.deduite(Geste.FICHE_SUIVI, null, null)).isEqualTo(VueOperation.MISE_EN_SERVICE);
        assertThat(VueOperation.deduite(Geste.DEPOSER_PREUVE, Particularite.CEREMONIE_CLES, null)).isEqualTo(VueOperation.CEREMONIE);
        assertThat(VueOperation.deduite(Geste.EVENEMENT_EXTERNE, null, Declencheurs.ATTESTATIONS_ARCHIVEES)).isEqualTo(VueOperation.ATTESTATIONS);
        assertThat(VueOperation.deduite(Geste.EVENEMENT_EXTERNE, null, Declencheurs.PV_SIGNE)).isEqualTo(VueOperation.DECISION);
        assertThat(VueOperation.deduite(Geste.DEPOSER_LIVRABLE, null, " ")).isEqualTo(VueOperation.AUCUNE);
        assertThat(VueOperation.CEREMONIE.libelle()).isNotBlank();
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        assertThat(p.operation("6").orElseThrow().getVue()).isEqualTo(VueOperation.AFFECTATION);
        assertThat(p.operation("C1").orElseThrow().getVue()).isEqualTo(VueOperation.CEREMONIE);
    }

    @Test
    @DisplayName("Compteurs décrits en données : lus du fichier livré, un document vide n'en donne aucun")
    void compteurs() {
        assertThat(DefinitionCompteur.parDefaut()).extracting(DefinitionCompteur::cle).contains("mesProjets", "tachesEnRetard");
        assertThat(DefinitionCompteur.lire("[]")).isEmpty();
        assertThatThrownBy(() -> DefinitionCompteur.charger("/absente.json")).isInstanceOf(IllegalStateException.class);
        DefinitionCompteur d = DefinitionCompteur.lire("[{\"cle\":\"x\",\"portee\":\"TACHE\"}]").get(0);
        assertThat(d.fonctions()).isEmpty();
        assertThat(d.liaison()).isNull();
    }
}
