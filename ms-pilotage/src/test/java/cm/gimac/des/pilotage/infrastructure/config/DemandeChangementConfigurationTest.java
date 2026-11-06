package cm.gimac.des.pilotage.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import cm.gimac.des.pilotage.domain.processus.EtatProcessus;
import cm.gimac.des.pilotage.domain.processus.ModeIntervenant;
import cm.gimac.des.pilotage.domain.processus.Processus;
import cm.gimac.des.pilotage.domain.processus.RegleInitiateur;
import cm.gimac.des.pilotage.domain.processus.TypeChamp;
import cm.gimac.des.pilotage.domain.processus.TypeChampFormulaire;
import cm.gimac.des.pilotage.domain.service.ControleProcessus;
import cm.gimac.des.pilotage.repository.HistoriqueProcessusRepository;
import cm.gimac.des.pilotage.repository.ProcessusRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class DemandeChangementConfigurationTest {
    @Test
    void modeleCompletPublieUneSeuleFois() {
        ProcessusRepository processus = mock(ProcessusRepository.class);
        HistoriqueProcessusRepository historique = mock(HistoriqueProcessusRepository.class);
        TransactionTemplate transactions = new TransactionTemplate(mock(PlatformTransactionManager.class));
        DemandeChangementConfiguration configuration = new DemandeChangementConfiguration(processus, historique, transactions, Clock.fixed(Instant.parse("2026-10-28T08:00:00Z"), ZoneOffset.UTC));
        configuration.run(null);
        ArgumentCaptor<Processus> capt = ArgumentCaptor.forClass(Processus.class);
        verify(processus).save(capt.capture());
        Processus p = capt.getValue();
        assertThat(p.getCode()).isEqualTo(DemandeChangementConfiguration.CODE);
        assertThat(p.getEtat()).isEqualTo(EtatProcessus.PUBLIEE);
        assertThat(p.getReferenceModele()).isEqualTo(DemandeChangementConfiguration.REFERENCE);
        assertThat(p.getInitiateurs().regles()).containsExactly(RegleInitiateur.TOUT_LE_GIMAC);
        assertThat(p.getInitiateurs().initiateurIntervenant()).isTrue();
        assertThat(p.getFormulaire().champs()).hasSize(12);
        assertThat(p.getFormulaire().champs()).extracting(c -> c.type()).contains(TypeChampFormulaire.LISTE, TypeChampFormulaire.TEXTE, TypeChampFormulaire.TEXTE_LONG);
        assertThat(p.getEtapes()).extracting(e -> e.code()).containsExactly("IDENTIFICATION", "RAISON", "IMPACT", "IMPLEMENTATION", "SSI");
        assertThat(p.getEtapes().subList(0, 3)).allMatch(e -> e.intervenant().mode() == ModeIntervenant.LANCEUR);
        assertThat(p.getEtapes().get(3).intervenant().mode()).isEqualTo(ModeIntervenant.AU_LANCEMENT);
        assertThat(p.getEtapes().get(3).livrables().get(0).obligatoire()).isTrue();
        assertThat(p.getEtapes().get(4).intervenant().profil()).isEqualTo("ROLE_RSSI");
        assertThat(p.getEtapes().get(4).signatures().get(0).signataire().profil()).isEqualTo("ROLE_RSSI");
        assertThat(p.getEtapes().get(4).signatures()).hasSize(1);
        assertThat(p.getEtapes().get(4).champs()).extracting(c -> c.type()).containsExactly(TypeChamp.DATE, TypeChamp.DATE, TypeChamp.TEXTE, TypeChamp.CHOIX, TypeChamp.TEXTE);
        // D-142 : seul avertissement, levable, la mise en œuvre ouverte à toute personne interne.
        assertThat(ControleProcessus.structure(p.getEtapes())).extracting(a -> a.code()).containsExactly("DESIGNATION_OUVERTE");
        verify(historique).save(any());
        when(processus.existsByCode(anyString())).thenReturn(true);
        configuration.run(null);
        verify(processus).save(any());
    }

    static final Instant LE = Instant.parse("2026-11-04T08:00:00Z");

    /** Version publiée du modèle système du 28/10 : gabarit daté, agent SSI à l'étape SSI. */
    static Processus publieeDatee(String gabarit) {
        Processus p = Processus.brouillon(DemandeChangementConfiguration.CODE, DemandeChangementConfiguration.UNITE, "systeme", Instant.parse("2026-10-28T08:00:00Z"));
        List<cm.gimac.des.pilotage.domain.processus.Etape> agent = DemandeChangementConfiguration.etapes().stream().map(e -> !"SSI".equals(e.code()) ? e : new cm.gimac.des.pilotage.domain.processus.Etape(
            e.code(), e.libelle(), e.action(), new cm.gimac.des.pilotage.domain.processus.Intervenant(ModeIntervenant.PROFIL, "ROLE_AGENT_SSI", null, "SSI", null), e.validateur(), e.predecesseurs(), e.delaiJours(), e.livrables(),
            e.signatures().stream().map(x -> new cm.gimac.des.pilotage.domain.processus.SignatureRequise(x.code(), x.libelle(), new cm.gimac.des.pilotage.domain.processus.Intervenant(ModeIntervenant.PROFIL, "ROLE_AGENT_SSI", null, "SSI", null), x.porte(), x.ordre())).toList(),
            e.champs(), e.position(), e.delaiMaxJours())).toList();
        p.decrire("Demande de changement", "Fiche RFC", "Exploitation", 10, agent, "systeme", Instant.parse("2026-10-28T08:00:00Z"));
        p.definirReference(gabarit);
        p.publier("systeme", "Modèle initial de la fiche RFC", false, Instant.parse("2026-10-28T08:00:00Z"));
        return p;
    }

    @Test
    void versionDateeRemplaceeParUneNouvelleVersionPubliee() {
        ProcessusRepository processus = mock(ProcessusRepository.class);
        HistoriqueProcessusRepository historique = mock(HistoriqueProcessusRepository.class);
        Processus ancienne = publieeDatee(DemandeChangementConfiguration.REFERENCE_DATEE);
        when(processus.existsByCode(DemandeChangementConfiguration.CODE)).thenReturn(true);
        when(processus.findFirstByCodeAndEtat(DemandeChangementConfiguration.CODE, EtatProcessus.PUBLIEE)).thenReturn(java.util.Optional.of(ancienne));
        when(processus.derniereVersion(DemandeChangementConfiguration.CODE)).thenReturn(1);
        new DemandeChangementConfiguration(processus, historique, new TransactionTemplate(mock(PlatformTransactionManager.class)), Clock.fixed(LE, ZoneOffset.UTC)).run(null);

        // L'ancienne version n'est pas réutilisée : retirée telle quelle, gabarit et agent SSI compris.
        assertThat(ancienne.getEtat()).isEqualTo(EtatProcessus.RETIREE);
        assertThat(ancienne.getReferenceModele()).isEqualTo(DemandeChangementConfiguration.REFERENCE_DATEE);
        assertThat(ancienne.getEtapes().get(4).intervenant().profil()).isEqualTo("ROLE_AGENT_SSI");
        verify(processus).saveAndFlush(ancienne);
        ArgumentCaptor<Processus> capt = ArgumentCaptor.forClass(Processus.class);
        verify(processus).save(capt.capture());
        Processus nouvelle = capt.getValue();
        assertThat(nouvelle).isNotSameAs(ancienne);
        assertThat(nouvelle.getVersion()).isEqualTo(2);
        assertThat(nouvelle.getEtat()).isEqualTo(EtatProcessus.PUBLIEE);
        assertThat(nouvelle.getReferenceModele()).isEqualTo(DemandeChangementConfiguration.REFERENCE);
        assertThat(nouvelle.getEtapes().get(4).intervenant().profil()).isEqualTo("ROLE_RSSI");
        assertThat(nouvelle.getEtapes().get(4).signatures().get(0).signataire().profil()).isEqualTo("ROLE_RSSI");
        assertThat(nouvelle.getEtapes().subList(0, 4)).isEqualTo(ancienne.getEtapes().subList(0, 4));
        assertThat(nouvelle.getFormulaire()).isEqualTo(ancienne.getFormulaire());
        verify(historique, times(2)).save(any());
    }

    @Test
    void versionAJourGabaritPersonnaliseOuBrouillonLaissesTelsQuels() {
        // Modèle système à jour (format de la fiche, RSSI, mise en œuvre ouverte) ou gabarit personnalisé : inchangés.
        for (Processus publiee : List.of(publieeAvecMiseEnOeuvre(null), publieeDatee("RFC-{AAAA}-{MM}-{JJ}-{NNNNNN}"))) {
            ProcessusRepository processus = mock(ProcessusRepository.class);
            when(processus.existsByCode(anyString())).thenReturn(true);
            when(processus.findFirstByCodeAndEtat(DemandeChangementConfiguration.CODE, EtatProcessus.PUBLIEE)).thenReturn(java.util.Optional.of(publiee));
            new DemandeChangementConfiguration(processus, mock(HistoriqueProcessusRepository.class), new TransactionTemplate(mock(PlatformTransactionManager.class)), Clock.fixed(LE, ZoneOffset.UTC)).run(null);
            assertThat(publiee.getEtat()).isEqualTo(EtatProcessus.PUBLIEE);
            verify(processus, never()).save(any());
        }
        ProcessusRepository processus = mock(ProcessusRepository.class);
        Processus ancienne = publieeDatee(DemandeChangementConfiguration.REFERENCE_DATEE);
        when(processus.existsByCode(anyString())).thenReturn(true);
        when(processus.findFirstByCodeAndEtat(DemandeChangementConfiguration.CODE, EtatProcessus.PUBLIEE)).thenReturn(java.util.Optional.of(ancienne));
        when(processus.findFirstByCodeAndEtat(DemandeChangementConfiguration.CODE, EtatProcessus.BROUILLON)).thenReturn(java.util.Optional.of(Processus.brouillon(DemandeChangementConfiguration.CODE, "SSI", "u", LE)));
        new DemandeChangementConfiguration(processus, mock(HistoriqueProcessusRepository.class), new TransactionTemplate(mock(PlatformTransactionManager.class)), Clock.fixed(LE, ZoneOffset.UTC)).run(null);
        assertThat(ancienne.getEtat()).isEqualTo(EtatProcessus.PUBLIEE);
        verify(processus, never()).save(any());
        verify(processus, never()).saveAndFlush(any());
    }

    @Test
    void etapesNonSsiEtIntervenantsHorsProfilInchanges() {
        List<cm.gimac.des.pilotage.domain.processus.Etape> etapes = DemandeChangementConfiguration.etapes();
        List<cm.gimac.des.pilotage.domain.processus.Etape> rssi = DemandeChangementConfiguration.rssi(etapes);
        assertThat(rssi.subList(0, 4)).isEqualTo(etapes.subList(0, 4));
        assertThat(rssi.get(4)).isEqualTo(etapes.get(4));
        var lanceur = new cm.gimac.des.pilotage.domain.processus.Etape("SSI", "SSI", "", new cm.gimac.des.pilotage.domain.processus.Intervenant(ModeIntervenant.LANCEUR, null, null, null, null), null, List.of(), 1, List.of(), List.of(), List.of(), null);
        assertThat(DemandeChangementConfiguration.rssi(List.of(lanceur)).get(0).intervenant().mode()).isEqualTo(ModeIntervenant.LANCEUR);
        var sansSignataire = new cm.gimac.des.pilotage.domain.processus.Etape("SSI", "SSI", "", null, null, List.of(), 1, List.of(),
            List.of(new cm.gimac.des.pilotage.domain.processus.SignatureRequise("DECISION_SSI", "Décision", null, List.of(), 1)), List.of(), null);
        var migree = DemandeChangementConfiguration.rssi(List.of(sansSignataire)).get(0);
        assertThat(migree.intervenant()).isNull();
        assertThat(migree.signatures().get(0).signataire()).isNull();
    }

    /** Modèle système publié au format CHG-{AAAA}-{N}, mise en œuvre désignée au lancement avec le profil {@code remplacement}. */
    static Processus publieeAvecMiseEnOeuvre(String remplacement) {
        Instant le = Instant.parse("2026-11-04T08:00:00Z");
        List<cm.gimac.des.pilotage.domain.processus.Etape> etapes = DemandeChangementConfiguration.etapes().stream().map(e -> !"IMPLEMENTATION".equals(e.code()) ? e : new cm.gimac.des.pilotage.domain.processus.Etape(
            e.code(), e.libelle(), e.action(), new cm.gimac.des.pilotage.domain.processus.Intervenant(ModeIntervenant.AU_LANCEMENT, null, null, DemandeChangementConfiguration.UNITE, remplacement), e.validateur(), e.predecesseurs(), e.delaiJours(), e.livrables(), e.signatures(), e.champs(), e.position(), e.delaiMaxJours())).toList();
        Processus p = Processus.brouillon(DemandeChangementConfiguration.CODE, DemandeChangementConfiguration.UNITE, "systeme", le);
        p.decrire("Demande de changement", "Fiche RFC", "Exploitation", 10, etapes, "systeme", le);
        p.definirReference(DemandeChangementConfiguration.REFERENCE);
        p.publier("systeme", "Modèle", false, le);
        return p;
    }

    @Test
    void miseEnOeuvreReserveeALaProductionMonetiqueRemplaceeSeulementPourLeModeleSysteme() {
        // D-142 : le modèle publié sur le serveur restreignait la mise en œuvre à ROLE_PRODUCTION_MONETIQUE.
        Processus ancien = publieeAvecMiseEnOeuvre("ROLE_PRODUCTION_MONETIQUE");
        assertThat(DemandeChangementConfiguration.aRemplacer(ancien)).isTrue();
        assertThat(DemandeChangementConfiguration.aJour(ancien.getEtapes()).get(3).intervenant().remplacement()).isNull();
        // Modèle à jour : rien à remplacer ; modèle personnalisé (autre profil) : jamais touché.
        assertThat(DemandeChangementConfiguration.aRemplacer(publieeAvecMiseEnOeuvre(null))).isFalse();
        assertThat(DemandeChangementConfiguration.aRemplacer(publieeAvecMiseEnOeuvre("ROLE_POLE_SYSTEME"))).isFalse();
        // Le modèle provisionné laisse la désignation ouverte, et seul l'avertissement levable DESIGNATION_OUVERTE le signale.
        assertThat(DemandeChangementConfiguration.etapes().get(3).intervenant()).matches(i -> i.mode() == ModeIntervenant.AU_LANCEMENT && i.remplacement() == null);
        assertThat(ControleProcessus.structure(DemandeChangementConfiguration.etapes())).extracting(a -> a.code()).containsExactly("DESIGNATION_OUVERTE");
    }

    @Test
    void miseEnOeuvreOuverteSeulementPourLeModeleSystemeDAvant() {
        var lanceur = new cm.gimac.des.pilotage.domain.processus.Intervenant(ModeIntervenant.LANCEUR, null, null, null, null);
        var profil = new cm.gimac.des.pilotage.domain.processus.Intervenant(ModeIntervenant.PROFIL, "ROLE_PRODUCTION_MONETIQUE", null, null, null);
        var ancien = new cm.gimac.des.pilotage.domain.processus.Intervenant(ModeIntervenant.AU_LANCEMENT, null, null, "SSI", "ROLE_PRODUCTION_MONETIQUE");
        List<cm.gimac.des.pilotage.domain.processus.Etape> l = List.of(
            new cm.gimac.des.pilotage.domain.processus.Etape("AUTRE", "Autre", "", ancien, null, List.of(), 1, List.of(), List.of(), List.of(), null),
            new cm.gimac.des.pilotage.domain.processus.Etape("IMPLEMENTATION", "Mise en œuvre", "", null, null, List.of(), 1, List.of(), List.of(), List.of(), null),
            new cm.gimac.des.pilotage.domain.processus.Etape("IMPLEMENTATION", "Mise en œuvre", "", lanceur, null, List.of(), 1, List.of(), List.of(), List.of(), null),
            new cm.gimac.des.pilotage.domain.processus.Etape("IMPLEMENTATION", "Mise en œuvre", "", profil, null, List.of(), 1, List.of(), List.of(), List.of(), null),
            new cm.gimac.des.pilotage.domain.processus.Etape("IMPLEMENTATION", "Mise en œuvre", "", ancien, null, List.of(), 1, List.of(), List.of(), List.of(), null)
        );
        List<cm.gimac.des.pilotage.domain.processus.Etape> r = DemandeChangementConfiguration.ouvrirMiseEnOeuvre(l);
        assertThat(r.subList(0, 4)).isEqualTo(l.subList(0, 4));
        assertThat(r.get(4).intervenant().remplacement()).isNull();
        assertThat(r.get(4).intervenant().unite()).isEqualTo("SSI");
    }
}

