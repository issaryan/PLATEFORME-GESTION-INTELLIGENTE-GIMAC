package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.application.port.out.RenduPdf;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.organisation.TypeUnite;
import cm.gimac.des.iam.domain.organisation.Unite;
import cm.gimac.des.iam.domain.service.TraitementAvatar.ImageAvatar;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** D-102 §4 : organigramme en PDF composé depuis les données de MS-IAM, paramètres, périmètre, nom du fichier. */
@ExtendWith(MockitoExtension.class)
class OrganigrammePdfServiceTest {

    static final byte[] PDF = "%PDF-1.7 organigramme".getBytes(StandardCharsets.US_ASCII);

    /** Moteur simulé : garde le dernier document reçu. */
    static final class RenduCapture implements RenduPdf {

        String html;
        String pied;
        Mise mise;

        @Override
        public byte[] rendre(String html, String piedDePage, Mise mise) {
            this.html = html;
            this.pied = piedDePage;
            this.mise = mise;
            return PDF;
        }
    }

    @Mock
    AvatarService avatars;

    AnnuaireEnMemoire annuaire;
    OrganisationServiceTest.CatalogueEnMemoire unites;
    RenduCapture rendu;
    OrganigrammePdfService service;
    String chef;

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(AnnuaireEnMemoire.catalogueParDefaut(AdministrationComptesServiceTest.CATALOGUE));
        chef = annuaire.creer(new NouveauCompte("chef", "Christian", "Ndong", "chef@gimac.test", null, "Chef du DES", null, Set.of("ROLE_CHEF_DES")), List.of());
        String agent = annuaire.creer(new NouveauCompte("agent", "Rachel", "Biyong", "agent@gimac.test", null, "Analyste", null, Set.of("ROLE_SNA")), List.of());
        String parti = annuaire.creer(new NouveauCompte("parti", "Kevin", "Ateba", "parti@gimac.test", null, "Ancien", null, Set.of()), List.of());
        String externe = annuaire.creer(new NouveauCompte("externe", "Brice", "Nzeng", "externe@gimac.test", null, "Correspondant", "INST-0042", Set.of()), List.of());
        annuaire.definirRattachement(chef, "DES", null);
        annuaire.definirRattachement(agent, "SNA", chef);
        annuaire.definirRattachement(parti, "SNA", chef);
        annuaire.definirActif(parti, false);
        unites = new OrganisationServiceTest.CatalogueEnMemoire();
        unites.enregistrer(new Unite("DG", "Direction Générale", TypeUnite.DIRECTION, null, true));
        unites.enregistrer(new Unite("DES", "Département Exploitation et Supervision", TypeUnite.DEPARTEMENT, "DG", true, chef));
        unites.enregistrer(new Unite("SNA", "Service Normes et Agréments", TypeUnite.SERVICE, "DES", true));
        unites.enregistrer(new Unite("ANCIEN", "Service dissous", TypeUnite.SERVICE, "DES", false));
        rendu = new RenduCapture();
        // 23 h 30 UTC le 9 octobre : déjà le 10 octobre à Douala (UTC+1).
        Clock horloge = Clock.fixed(Instant.parse("2026-10-09T23:30:00Z"), ZoneOffset.UTC);
        service = new OrganigrammePdfService(unites, annuaire, avatars, rendu, horloge);
        lenient().when(avatars.lire(anyString())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Toute l'organisation : unités actives, personnes internes actives, photo du chef, nom daté de Douala, A3 paysage par défaut")
    void touteLOrganisation() {
        when(avatars.lire(chef)).thenReturn(Optional.of(new ImageAvatar(new byte[] { 1, 2, 3 }, "image/webp", "v1")));
        OrganigrammePdfService.Fichier f = service.produire(OrganigrammePdfService.demande(null, null, null, true));
        assertThat(f.nom()).isEqualTo("organigramme-moabi-2026-10-10.pdf");
        assertThat(f.contenu()).isEqualTo(PDF);
        assertThat(rendu.mise).isEqualTo(new RenduPdf.Mise(RenduPdf.Format.A3, true));
        assertThat(rendu.html).contains("Toute l'organisation · 3 unités · 2 personnes").contains("Édité le 10 octobre 2026 à 00:30 (heure de Douala)");
        assertThat(rendu.html).contains("Christian Ndong").contains("Rachel Biyong").doesNotContain("Kevin Ateba").doesNotContain("Brice Nzeng").doesNotContain("Service dissous");
        assertThat(rendu.html).contains("src=\"data:image/webp;base64,AQID\"").contains("Membres par unité");
        assertThat(rendu.pied).contains("totalPages");
    }

    @Test
    @DisplayName("Branche, A4 portrait sans annexe ; racine inconnue : 404 ORG_UNITE_INTROUVABLE")
    void branche() {
        service.produire(OrganigrammePdfService.demande(" DES ", "a4", "Portrait", false));
        assertThat(rendu.mise).isEqualTo(new RenduPdf.Mise(RenduPdf.Format.A4, false));
        assertThat(rendu.html).contains("Branche « Département Exploitation et Supervision » · 2 unités").doesNotContain("Direction Générale").doesNotContain("Membres par unité");
        assertThatThrownBy(() -> service.produire(OrganigrammePdfService.demande("INCONNUE", null, null, false)))
            .isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("ORG_UNITE_INTROUVABLE"));
    }

    @Test
    @DisplayName("Paramètres : format A4 ou A3, orientation paysage ou portrait, sinon 400 ORG_PDF_PARAMETRE")
    void parametres() {
        assertThat(OrganigrammePdfService.demande("", "A4", "paysage", true)).isEqualTo(new OrganigrammePdfService.Demande(null, RenduPdf.Format.A4, true, true));
        for (String[] p : new String[][] { { "A5", "paysage" }, { "A3", "oblique" } }) {
            assertThatThrownBy(() -> OrganigrammePdfService.demande(null, p[0], p[1], false)).isInstanceOfSatisfying(IamException.class, e -> {
                assertThat(e.code()).isEqualTo("ORG_PDF_PARAMETRE");
                assertThat(e.status()).isEqualTo(400);
            });
        }
        assertThat(new RenduPdf.Mise(RenduPdf.Format.A4, true).largeurMm()).isEqualTo(297);
        assertThat(new RenduPdf.Mise(RenduPdf.Format.A4, false).hauteurMm()).isEqualTo(297);
    }
}
