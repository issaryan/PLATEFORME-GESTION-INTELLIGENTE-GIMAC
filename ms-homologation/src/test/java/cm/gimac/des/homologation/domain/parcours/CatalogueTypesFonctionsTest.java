package cm.gimac.des.homologation.domain.parcours;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Types de projet et fonctions en données (D-91) : valeurs, modèles, catalogue des fonctions. */
class CatalogueTypesFonctionsTest {

    @AfterEach
    void fin() {
        CatalogueFonctions.brancher(null);
    }

    @Test
    @DisplayName("Type de projet : code validé, trois types repris, ordre, égalité, sérialisation par le code")
    void typeProjet() {
        assertThat(TypeProjet.values()).containsExactly(TypeProjet.CARTE_INTERBANCAIRE, TypeProjet.PAIEMENT_MOBILE, TypeProjet.DELEGATAIRE);
        assertThat(TypeProjet.valueOf("PAIEMENT_MOBILE")).isSameAs(TypeProjet.PAIEMENT_MOBILE);
        TypeProjet me = TypeProjet.valueOf("MONNAIE_ELECTRONIQUE");
        assertThat(me).isEqualTo(TypeProjet.valueOf("MONNAIE_ELECTRONIQUE")).hasSameHashCodeAs(TypeProjet.valueOf("MONNAIE_ELECTRONIQUE")).isNotEqualTo("MONNAIE_ELECTRONIQUE");
        assertThat(me.repris()).isFalse();
        assertThat(TypeProjet.DELEGATAIRE.repris()).isTrue();
        assertThat(me.name()).isEqualTo(me.toString()).isEqualTo("MONNAIE_ELECTRONIQUE");
        List<TypeProjet> l = new ArrayList<>(List.of(TypeProjet.valueOf("ZZZ"), me, TypeProjet.DELEGATAIRE, TypeProjet.CARTE_INTERBANCAIRE));
        l.sort(null);
        assertThat(l).containsExactly(TypeProjet.CARTE_INTERBANCAIRE, TypeProjet.DELEGATAIRE, me, TypeProjet.valueOf("ZZZ"));
        for (String invalide : Arrays.asList(null, "ab", "1ABC", "minuscule", "A".repeat(41))) {
            assertThatThrownBy(() -> TypeProjet.valueOf(invalide)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("Modèle : attestations, catalogue des services, plateforme des essais, avancement, annexe A, modèle par défaut")
    void modeles() {
        assertThat(ModeleTypeProjet.CARTE_INTERBANCAIRE.attestationsExigees(false)).containsExactly(TypeAttestation.PCI_DSS);
        assertThat(ModeleTypeProjet.DELEGATAIRE.attestationsExigees(true)).containsExactly(TypeAttestation.PCI_DSS, TypeAttestation.COBAC);
        assertThat(ModeleTypeProjet.PAIEMENT_MOBILE.attestationsExigees(false)).containsExactly(TypeAttestation.ISO_27001, TypeAttestation.GSMA);
        assertThat(ModeleTypeProjet.GENERIQUE.attestationsExigees(false)).isEmpty();
        assertThat(ModeleTypeProjet.GENERIQUE.attestationsExigees(true)).containsExactly(TypeAttestation.COBAC);
        assertThat(ModeleTypeProjet.PAIEMENT_MOBILE.catalogueServices()).contains("CASH_IN");
        assertThat(ModeleTypeProjet.DELEGATAIRE.catalogueServices()).contains("RETRAIT");
        assertThat(ModeleTypeProjet.GENERIQUE.catalogueServices()).isEmpty();
        assertThat(ModeleTypeProjet.PAIEMENT_MOBILE.plateformeEssais()).isEqualTo(PlateformeExecution.SIMULATEUR);
        assertThat(ModeleTypeProjet.CARTE_INTERBANCAIRE.plateformeEssais()).isNull();
        assertThat(ModeleTypeProjet.PAIEMENT_MOBILE.avancementSimulateur()).isTrue();
        assertThat(ModeleTypeProjet.GENERIQUE.avancementSimulateur()).isFalse();
        assertThat(ModeleTypeProjet.CARTE_INTERBANCAIRE.annexeA()).contains(TypeProjet.CARTE_INTERBANCAIRE);
        assertThat(ModeleTypeProjet.GENERIQUE.annexeA()).isEmpty();
        assertThat(ModeleTypeProjet.parDefaut(TypeProjet.PAIEMENT_MOBILE)).isEqualTo(ModeleTypeProjet.PAIEMENT_MOBILE);
        assertThat(ModeleTypeProjet.parDefaut(TypeProjet.valueOf("AUTRE"))).isEqualTo(ModeleTypeProjet.GENERIQUE);
        assertThat(ModeleTypeProjet.parDefaut(null)).isEqualTo(ModeleTypeProjet.GENERIQUE);
        assertThat(ModeleTypeProjet.GENERIQUE.libelle()).isEqualTo("Générique");
        // Anciennes entrées, désormais portées par le modèle.
        assertThat(TypeAttestation.exigees(TypeProjet.PAIEMENT_MOBILE, false)).containsExactly(TypeAttestation.ISO_27001, TypeAttestation.GSMA);
        assertThat(CatalogueServices.parType(TypeProjet.CARTE_INTERBANCAIRE)).containsExactly("RETRAIT", "PAIEMENT", "TRANSFERT", "COMMERCE_ELECTRONIQUE");
    }

    @Test
    @DisplayName("Fonctions reprises : seize, profils, externes, libellés, profils des sessions")
    void fonctionsReprises() {
        assertThat(Fonction.values()).hasSize(16).startsWith(Fonction.DDRM).endsWith(Fonction.PERSONNALISATEUR);
        assertThat(Fonction.SNA.profils()).containsExactly("ROLE_SNA", "ROLE_INGENIEUR_HOMOLOGATION");
        assertThat(Fonction.PARTICIPANT.externe()).isTrue();
        assertThat(Fonction.CONDUCTEUR.externe()).isFalse();
        assertThat(Fonction.RSSI.libelle()).isEqualTo("Sécurité des systèmes d'information");
        assertThat(Fonction.DG.reprise()).isTrue();
        assertThat(Fonction.DG.connue()).isTrue();
        assertThat(Fonction.duProfil("ROLE_CHEF_DES")).contains(Fonction.CHEF_DES);
        assertThat(Fonction.duProfil("ROLE_INCONNU")).isEmpty();
        assertThat(Fonction.desProfils(Set.of("ROLE_PRODUCTION_MONETIQUE"))).containsExactly(Fonction.PRODUCTION_MONETIQUE, Fonction.EXPLOITATION);
        assertThat(Fonction.duCatalogue("JURISTE")).isEmpty();
        assertThat(Fonction.duCatalogue("x")).isEmpty();
        assertThat(Fonction.duCatalogue(null)).isEmpty();
        assertThat(Fonction.duCatalogue("DG")).contains(Fonction.DG);
        Fonction inconnue = Fonction.valueOf("JURISTE");
        assertThat(inconnue.connue()).isFalse();
        assertThat(inconnue.libelle()).isEqualTo("JURISTE");
        assertThat(inconnue.profils()).isEmpty();
        assertThat(CatalogueFonctions.active(inconnue)).isFalse();
        assertThat(inconnue).isEqualTo(Fonction.valueOf("JURISTE")).hasSameHashCodeAs(Fonction.valueOf("JURISTE")).isNotEqualTo("JURISTE");
        assertThat(inconnue.toString()).isEqualTo("JURISTE");
        List<Fonction> l = new ArrayList<>(List.of(Fonction.valueOf("ZETA"), inconnue, Fonction.DG, Fonction.DDRM));
        l.sort(null);
        assertThat(l).containsExactly(Fonction.DDRM, Fonction.DG, inconnue, Fonction.valueOf("ZETA"));
        assertThatThrownBy(() -> Fonction.valueOf("j")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Fonction.valueOf(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Catalogue branché : fonction créée lue par le domaine, écriture retenue, relecture, source illisible")
    void catalogueBranche() {
        AtomicInteger lectures = new AtomicInteger();
        List<CatalogueFonctions.Definition> source = new ArrayList<>(CatalogueFonctions.REPRISES);
        source.add(new CatalogueFonctions.Definition("JURISTE", "Juriste", List.of("ROLE_JURISTE"), false, true));
        source.add(new CatalogueFonctions.Definition("AUDITEUR_EXTERNE", "Auditeur externe", null, true, false));
        CatalogueFonctions.brancher(() -> {
            lectures.incrementAndGet();
            return source;
        });
        Fonction juriste = Fonction.valueOf("JURISTE");
        assertThat(Fonction.values()).hasSize(18).endsWith(Fonction.valueOf("JURISTE"));
        assertThat(juriste.profils()).containsExactly("ROLE_JURISTE");
        assertThat(Fonction.desProfils(Set.of("ROLE_JURISTE"))).containsExactly(juriste);
        assertThat(Fonction.duProfil("ROLE_JURISTE")).contains(juriste);
        assertThat(CatalogueFonctions.active(juriste)).isTrue();
        assertThat(Fonction.valueOf("AUDITEUR_EXTERNE").externe()).isTrue();
        assertThat(CatalogueFonctions.active(Fonction.valueOf("AUDITEUR_EXTERNE"))).isFalse();
        assertThat(lectures.get()).isEqualTo(1);
        // Écriture de cette instance : visible aussitôt, sans relecture.
        CatalogueFonctions.retenir(new CatalogueFonctions.Definition("JURISTE", "Juriste principal", List.of("ROLE_JURISTE"), false, false));
        assertThat(juriste.libelle()).isEqualTo("Juriste principal");
        assertThat(CatalogueFonctions.active(juriste)).isFalse();
        assertThat(lectures.get()).isEqualTo(1);
        // Après la transaction : relecture de la source.
        CatalogueFonctions.invalider();
        assertThat(juriste.libelle()).isEqualTo("Juriste");
        assertThat(lectures.get()).isEqualTo(2);
        // Source illisible : la dernière lecture est gardée ; sans lecture antérieure, les fonctions reprises.
        CatalogueFonctions.brancher(() -> {
            throw new IllegalStateException("base indisponible");
        });
        assertThat(CatalogueFonctions.toutes()).hasSize(16);
        assertThat(CatalogueFonctions.trouver("JURISTE")).isEmpty();
        CatalogueFonctions.retenir(new CatalogueFonctions.Definition("JURISTE", "Juriste", List.of("ROLE_JURISTE"), false, true));
        CatalogueFonctions.invalider();
        assertThat(CatalogueFonctions.toutes()).hasSize(17);
        assertThat(CatalogueFonctions.definition(Fonction.DG).libelle()).isEqualTo("Direction générale");
        assertThat(CatalogueFonctions.FRAICHEUR).isPositive();
    }
}
