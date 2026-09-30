package cm.gimac.des.homologation.application.usecase;

import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.avancer;
import static cm.gimac.des.homologation.domain.parcours.ParcoursSamples.parcours;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.usecase.EvenementsCertification.Signature;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.parcours.EtatApprobation;
import cm.gimac.des.homologation.domain.parcours.EtatProcesVerbal;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.StatutParcours;
import cm.gimac.des.homologation.domain.parcours.TypeAttestation;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Effets des événements de MS-CERTIFICATION sur le parcours et sa projection des verrous. */
@ExtendWith(MockitoExtension.class)
class EvenementsCertificationTest {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-06T09:00:00Z"), ZoneOffset.UTC);

    @Mock
    ParcoursDepot depot;

    @Test
    @DisplayName("Procès-verbal établi puis signé, dossier et fiche de changement, attestations, approbation")
    void sequence() {
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        avancer(p, "17", LocalDate.parse("2026-10-01"));
        when(depot.trouver(p.id(), true)).thenReturn(Optional.of(p));
        EvenementsCertification e = new EvenementsCertification(depot, CLOCK);
        e.pvEtabli(p.id(), "Conducteur", "f".repeat(64));
        assertThat(p.operation("18").orElseThrow().getPreuve().nom()).isEqualTo("proces-verbal-certification-LIA-2026-001.pdf");
        e.pvSigne(p.id(), List.of(new Signature("CHEF_DES", "Chef DES"), new Signature("PARTICIPANT", " "), new Signature(null, null)), "f".repeat(64));
        assertThat(p.etat().getEtatPv()).isEqualTo(EtatProcesVerbal.SIGNE);
        assertThat(p.etat().getStatut()).isEqualTo(StatutParcours.CERTIFIEE);
        assertThat(p.operation("19").orElseThrow().getPreuve().deposePar()).isEqualTo("Chef DES");
        e.dossierChangementConstitue(p.id(), "CHG-DOS-1", "dossier.pdf", "a".repeat(64));
        e.ficheChangementReferencee(p.id(), "CHG-2026-0412");
        e.attestationDeposee(p.id(), TypeAttestation.PCI_DSS);
        e.attestationVerifiee(p.id(), TypeAttestation.PCI_DSS);
        e.visasAttendus(p.id(), List.of("CONFORMITE"));
        e.visaSigne(p.id(), "CONFORMITE");
        e.attestationsArchivees(p.id(), List.of("ATT-1", "ATT-2"));
        assertThat(p.operation("21").orElseThrow().getPreuve().documentRef()).isEqualTo("ATT-1");
        e.securiteApprouvee(p.id(), "2026-10-06");
        assertThat(p.etat().getEtatApprobation()).isEqualTo(EtatApprobation.APPROUVEE);
        assertThat(p.etat().getStatut()).isEqualTo(StatutParcours.APPROUVEE_SECURITE);
    }

    @Test
    @DisplayName("Liaison sans parcours : événement sans effet ; archivage sans référence")
    void inconnue() {
        UUID id = UUID.randomUUID();
        when(depot.trouver(id, true)).thenReturn(Optional.empty());
        EvenementsCertification e = new EvenementsCertification(depot, CLOCK);
        e.pvEtabli(id, "x", "y");
        Parcours p = parcours(TypeProjet.CARTE_INTERBANCAIRE, "RETRAIT");
        when(depot.trouver(p.id(), true)).thenReturn(Optional.of(p));
        e.attestationsArchivees(p.id(), List.of());
        assertThat(p.operation("21").orElseThrow().getPreuve().documentRef()).isEqualTo("ATT-LIA-2026-001");
    }
}
