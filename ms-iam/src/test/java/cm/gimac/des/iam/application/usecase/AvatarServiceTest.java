package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.service.TraitementAvatarTest;
import cm.gimac.des.iam.infrastructure.idp.AnnuaireEnMemoire;
import cm.gimac.des.iam.infrastructure.persistence.AvatarCompte;
import cm.gimac.des.iam.infrastructure.persistence.AvatarCompteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Photo de profil (docs/lot1/mon-compte.md §1) : remplacement, retrait, lecture, versions, traces. */
@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    static final Instant MAINTENANT = Instant.parse("2026-10-01T09:00:00Z");

    @Mock
    AvatarCompteRepository depot;

    @Mock
    JournalActes journal;

    AnnuaireEnMemoire annuaire;
    AvatarService service;
    Acteur titulaire;

    @BeforeEach
    void init() {
        annuaire = new AnnuaireEnMemoire(Map.of("ROLE_CHARGE_DDRM", Set.of("PAR_INSTITUTION_READ")), Set.of());
        String id = annuaire.creer(new NouveauCompte("agent", "A", "B", "agent@gimac.test", null, null, null, Set.of("ROLE_CHARGE_DDRM")), List.of());
        titulaire = new Acteur(id, "Agent", Set.of());
        service = new AvatarService(depot, annuaire, journal, Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Première photo : image réencodée enregistrée, version rendue, journal et événement sans l'image")
    void premiere() throws Exception {
        when(depot.findById(titulaire.id())).thenReturn(Optional.empty());
        String version = service.remplacer(titulaire, TraitementAvatarTest.png(300, 300));
        ArgumentCaptor<AvatarCompte> c = ArgumentCaptor.forClass(AvatarCompte.class);
        verify(depot).save(c.capture());
        assertThat(c.getValue().getCompteId()).isEqualTo(titulaire.id());
        assertThat(c.getValue().getVersion()).isEqualTo(version);
        assertThat(c.getValue().getType()).isEqualTo("image/png");
        assertThat(c.getValue().getMisAJourLe()).isEqualTo(MAINTENANT);
        assertThat(TraitementAvatarTest.lire(c.getValue().getImage()).getWidth()).isEqualTo(256);
        verify(journal).avatar(titulaire, titulaire.id(), "agent", "REMPLACEE", version);
    }

    @Test
    @DisplayName("Remplacement : la ligne existante est mise à jour ; image refusée : rien n'est écrit")
    void remplacement() throws Exception {
        AvatarCompte existant = new AvatarCompte(titulaire.id(), new byte[] { 1 }, "image/png", "ancienne", Instant.EPOCH);
        when(depot.findById(titulaire.id())).thenReturn(Optional.of(existant));
        String version = service.remplacer(titulaire, TraitementAvatarTest.png(100, 100));
        verify(depot).save(existant);
        assertThat(existant.getVersion()).isEqualTo(version).isNotEqualTo("ancienne");
        assertThat(existant.getMisAJourLe()).isEqualTo(MAINTENANT);
        assertThatThrownBy(() -> service.remplacer(titulaire, new byte[] { 1, 2, 3 })).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_AVATAR_TYPE"));
        verify(journal, org.mockito.Mockito.times(1)).avatar(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Retrait : ligne supprimée et tracée ; sans photo, rien n'est tracé ; compte inconnu refusé")
    void retrait() {
        AvatarCompte existant = new AvatarCompte(titulaire.id(), new byte[] { 1 }, "image/png", "v1", Instant.EPOCH);
        when(depot.findById(titulaire.id())).thenReturn(Optional.of(existant), Optional.empty());
        service.retirer(titulaire);
        verify(depot).delete(existant);
        verify(journal).avatar(eq(titulaire), eq(titulaire.id()), eq("agent"), eq("RETIREE"), isNull());
        service.retirer(titulaire);
        verify(depot, org.mockito.Mockito.times(1)).delete(any());
        Acteur inconnu = new Acteur("inconnu", "x", Set.of());
        assertThatThrownBy(() -> service.retirer(inconnu)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_ACCOUNT_NOT_FOUND"));
        verify(depot, never()).findById("inconnu");
    }

    @Test
    @DisplayName("Lecture et versions : image conservée, version seule, versions d'un lot de comptes")
    void lecture() {
        when(depot.findById("u1")).thenReturn(Optional.of(new AvatarCompte("u1", new byte[] { 7 }, "image/png", "v1", Instant.EPOCH)));
        assertThat(service.lire("u1")).hasValueSatisfying(i -> {
            assertThat(i.octets()).containsExactly(7);
            assertThat(i.version()).isEqualTo("v1");
        });
        assertThat(service.lire(null)).isEmpty();
        when(depot.version("u1")).thenReturn(Optional.of("v1"));
        assertThat(service.version("u1")).contains("v1");
        assertThat(service.version(null)).isEmpty();
        AvatarCompteRepository.VersionAvatar v = new AvatarCompteRepository.VersionAvatar() {
            @Override
            public String getCompteId() {
                return "u1";
            }

            @Override
            public String getVersion() {
                return "v1";
            }
        };
        when(depot.versions(List.of("u1", "u2"))).thenReturn(List.of(v));
        assertThat(service.versions(List.of("u1", "u2"))).containsExactly(Map.entry("u1", "v1"));
        assertThat(service.versions(List.of())).isEmpty();
        assertThat(service.versions(null)).isEmpty();
    }

    @Test
    @DisplayName("Entité : copies défensives de l'image")
    void copies() {
        byte[] image = { 1, 2 };
        AvatarCompte a = new AvatarCompte("u", image, "image/png", "v", Instant.EPOCH);
        image[0] = 9;
        assertThat(a.getImage()).containsExactly(1, 2);
        a.getImage()[0] = 9;
        assertThat(a.getImage()).containsExactly(1, 2);
        a.remplacer(null, "image/png", "v2", Instant.EPOCH);
        assertThat(a.getImage()).isNull();
    }
}
