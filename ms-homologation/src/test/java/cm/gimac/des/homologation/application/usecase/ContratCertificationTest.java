package cm.gimac.des.homologation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.homologation.application.usecase.EvenementsCertification.Signature;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.Liaison;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;
import cm.gimac.des.homologation.domain.enumeration.Segment;
import cm.gimac.des.homologation.domain.event.OperationClosedPayload;
import cm.gimac.des.homologation.domain.event.OperationParcoursPayload;
import cm.gimac.des.homologation.domain.parcours.CatalogueFonctions;
import cm.gimac.des.homologation.domain.parcours.Declencheurs;
import cm.gimac.des.homologation.domain.parcours.EtatOperationParcours;
import cm.gimac.des.homologation.domain.parcours.Fonction;
import cm.gimac.des.homologation.domain.parcours.Geste;
import cm.gimac.des.homologation.domain.parcours.GrainPreuve;
import cm.gimac.des.homologation.domain.parcours.Jalon;
import cm.gimac.des.homologation.domain.parcours.Livrable;
import cm.gimac.des.homologation.domain.parcours.NatureLivrable;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.ParcoursLiaison;
import cm.gimac.des.homologation.domain.parcours.SignatureOperation;
import cm.gimac.des.homologation.domain.parcours.SignaturesSamples;
import cm.gimac.des.homologation.domain.service.NoeudReferentiel;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator.ExigencesCertification;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator.TypeAnomalie;
import cm.gimac.des.homologation.domain.service.TopologicalSorter;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Contrat avec MS-CERTIFICATION (docs/lot1/effet-reel-referentiels.md §8.2, §10.5) : déclencheur et signatures
 * configurées dans les événements d'opération, homologation.operation-closed.v1, signatures du procès-verbal reprises
 * de certification.pv-signed.v1 (pas de double signature), gestes de certification attendus à la publication.
 */
class ContratCertificationTest {

    static final Instant T = Instant.parse("2026-10-08T08:00:00Z");
    static final Clock CLOCK = Clock.fixed(T, ZoneOffset.UTC);

    @BeforeEach
    void init() {
        SignaturesSamples.brancherCatalogue();
    }

    @AfterEach
    void fin() {
        CatalogueFonctions.brancher(null);
    }

    /** Opération SIG close par certification.pv-signed.v1, avec deux signatures configurées (D-91). */
    static Parcours parcoursPv() {
        ReferentialVersion v = ReferentialVersion.nouveauBrouillon(SignaturesSamples.TYPE, "Agrégateur", Instant.parse("2026-10-01T00:00:00Z"));
        v.setId(UUID.randomUUID());
        v.upsertOperation("OUV", donnees(List.of(), null, null, List.of()));
        v.upsertOperation("SIG", donnees(List.of("OUV"), Declencheurs.PV_SIGNE, Jalon.CERTIFIEE, SignaturesSamples.signatures()));
        v.setNumero(1);
        Liaison l = new Liaison()
            .typeProjet(SignaturesSamples.TYPE)
            .institutionId(UUID.randomUUID())
            .institutionNom("Agrégateur Alpha")
            .etat(EtatLiaison.OUVERTE)
            .chefDeProjetId(SignaturesSamples.CONDUCTEUR)
            .dateOuverture(Instant.parse("2026-10-01T08:00:00Z"))
            .cycleAptitude(0)
            .version(v);
        l.setId(UUID.randomUUID());
        return Parcours.instancier(l, new ParcoursLiaison(l.getId(), "LIA-2026-092", SignaturesSamples.J), v.getOperations(), List.of(), SignaturesSamples.designation());
    }

    static ReferenceOperationData donnees(List<String> preds, String declencheur, Jalon jalon, List<SignatureOperation> signatures) {
        return new ReferenceOperationData(
            "Opération",
            null,
            Segment.TRONC_COMMUN_AMONT,
            null,
            null,
            null,
            null,
            null,
            preds,
            null,
            null,
            null,
            null,
            "LANCEMENT",
            1,
            Fonction.CONDUCTEUR,
            Fonction.CHEF_DES,
            List.of(),
            List.of(),
            List.of(),
            2,
            GrainPreuve.OPERATION,
            null,
            false,
            false,
            false,
            false,
            null,
            List.of(new Livrable("L1", "Compte rendu", NatureLivrable.LIVRABLE, Fonction.CONDUCTEUR, List.of("application/pdf"), true)),
            Geste.DEPOSER_LIVRABLE,
            jalon,
            declencheur,
            null,
            null,
            signatures
        );
    }

    @Test
    @DisplayName("Événements d'opération : declencheur et signatures configurées ; operation-closed.v1 à chaque clôture (FAITE, SANS_OBJET)")
    void evenements() {
        Parcours p = parcoursPv();
        EvenementsParcours ev = new EvenementsParcours(mock(OutboxPublisher.class), CLOCK);
        OperationParcours sig = p.operation("SIG").orElseThrow();
        OperationParcoursPayload a = ev.operation(p, sig, "OUVERTURE");
        assertThat(a.declencheur()).isEqualTo(Declencheurs.PV_SIGNE);
        assertThat(a.signatures())
            .extracting(OperationParcoursPayload.SignatureConfiguree::code, OperationParcoursPayload.SignatureConfiguree::signataire, OperationParcoursPayload.SignatureConfiguree::ordre)
            .containsExactly(org.assertj.core.groups.Tuple.tuple("VISA_JURIDIQUE", "JURISTE", 1), org.assertj.core.groups.Tuple.tuple("APPROBATION", "CONDUCTEUR", 2));
        assertThat(ev.operation(p, p.operation("OUV").orElseThrow(), null).signatures()).isEmpty();

        OutboxPublisher outbox = mock(OutboxPublisher.class);
        EvenementsParcours ev2 = new EvenementsParcours(outbox, CLOCK);
        Map<UUID, EtatOperationParcours> avant = new HashMap<>();
        p.operations().forEach(o -> avant.put(o.getId(), o.getEtat()));
        OperationParcours ouv = p.operation("OUV").orElseThrow();
        p.deposerLivrable(ouv, SignaturesSamples.piece("DOC-OUV"), SignaturesSamples.J);
        sig.setEtat(EtatOperationParcours.SANS_OBJET);
        ev2.emettre(p, avant, EnumSet.noneOf(Jalon.class));
        ArgumentCaptor<Object> charge = ArgumentCaptor.forClass(Object.class);
        verify(outbox, atLeastOnce()).publish(eq("liaison"), eq(p.id()), eq(OperationClosedPayload.TYPE), charge.capture(), anyString());
        assertThat(charge.getAllValues())
            .map(OperationClosedPayload.class::cast)
            .extracting(OperationClosedPayload::code, OperationClosedPayload::etat)
            .contains(org.assertj.core.groups.Tuple.tuple("OUV", "FAITE"), org.assertj.core.groups.Tuple.tuple("SIG", "SANS_OBJET"));
        OperationClosedPayload c = charge.getAllValues().stream().map(OperationClosedPayload.class::cast).filter(x -> x.code().equals("SIG")).findFirst().orElseThrow();
        assertThat(c.declencheur()).isEqualTo(Declencheurs.PV_SIGNE);
        assertThat(c.liaisonId()).isEqualTo(p.id());
        assertThat(c.le()).isEqualTo(T);
    }

    @Test
    @DisplayName("pv-signed.v1 lu comme complet : signatures D-91 reprises par code, opération close sans EN_SIGNATURE ; signature non recueillie : attendue")
    void procesVerbalSigne() {
        Parcours p = parcoursPv();
        ParcoursDepot depot = mock(ParcoursDepot.class);
        when(depot.trouver(p.id(), true)).thenReturn(Optional.of(p));
        EvenementsCertification e = new EvenementsCertification(depot, CLOCK);
        e.pvSigne(
            p.id(),
            List.of(new Signature("CHEF_DES", "Chef DES", "2026-10-08T07:00:00Z", "VISA_JURIDIQUE"), new Signature("CONDUCTEUR", "Conducteur", "2026-10-08T07:30:00Z", "APPROBATION")),
            "f".repeat(64)
        );
        OperationParcours sig = p.operation("SIG").orElseThrow();
        assertThat(sig.getEtat()).isEqualTo(EtatOperationParcours.FAITE);
        assertThat(sig.getSignaturesRecueillies()).extracting(r -> r.code() + ":" + r.origine() + ":" + r.signataireNom() + ":" + r.fonction()).containsExactly(
            "VISA_JURIDIQUE:certification.pv-signed.v1:Chef DES:JURISTE",
            "APPROBATION:certification.pv-signed.v1:Conducteur:CONDUCTEUR"
        );
        assertThat(sig.getSignaturesRecueillies().get(0).signeLe()).isEqualTo(Instant.parse("2026-10-08T07:00:00Z"));

        // Une seule signature du circuit correspond : l'autre reste attendue (jamais inventée).
        Parcours q = parcoursPv();
        when(depot.trouver(q.id(), true)).thenReturn(Optional.of(q));
        e.pvSigne(q.id(), List.of(new Signature("CHEF_DES", "Chef DES", "illisible", "VISA_JURIDIQUE"), new Signature("PARTICIPANT", null)), "f".repeat(64));
        OperationParcours sq = q.operation("SIG").orElseThrow();
        assertThat(sq.getEtat()).isEqualTo(EtatOperationParcours.EN_SIGNATURE);
        assertThat(sq.signaturesAttendues()).extracting(SignatureOperation::code).containsExactly("APPROBATION");
        assertThat(sq.getSignaturesRecueillies().get(0).signeLe()).isEqualTo(T);
    }

    @Test
    @DisplayName("Publication : GESTE_CERTIFICATION_ABSENT quand un jalon est porté sans les déclencheurs attendus (configuration)")
    void gestesCertification() {
        ExigencesCertification ex = new ExigencesCertification(
            Map.of(Jalon.CERTIFIEE, List.of(Declencheurs.PV_ETABLI, Declencheurs.PV_SIGNE)),
            Map.of(Jalon.CERTIFIEE, List.of("certification.attestations-archived.v1"))
        );
        ReferentialGraphValidator v = new ReferentialGraphValidator(new TopologicalSorter(), ex);
        ReferentialVersion version = parcoursPv().liaison().getVersion();
        List<NoeudReferentiel> noeuds = version.noeuds();
        var r = v.controler(noeuds, false);
        assertThat(r.anomalies()).filteredOn(x -> x.type() == TypeAnomalie.GESTE_CERTIFICATION_ABSENT).singleElement().satisfies(x ->
            assertThat(x.codes()).containsExactly("CERTIFIEE", Declencheurs.PV_ETABLI)
        );
        assertThat(v.controler(noeuds, true).anomalies())
            .filteredOn(x -> x.type() == TypeAnomalie.GESTE_CERTIFICATION_ABSENT)
            .singleElement()
            .satisfies(x -> assertThat(x.codes()).containsExactly("CERTIFIEE", Declencheurs.PV_ETABLI, "certification.attestations-archived.v1"));
        assertThatThrownBy(() -> v.validate(noeuds, false)).isInstanceOfSatisfying(HomologationException.class, x -> assertThat(x.code()).isEqualTo("HOM_REFERENTIAL_GRAPH_INVALID"));
        // Sans configuration : aucun geste attendu.
        assertThat(new ReferentialGraphValidator().controler(noeuds).anomalies()).noneMatch(x -> x.type() == TypeAnomalie.GESTE_CERTIFICATION_ABSENT);
        // Configuration lue par jalon (déclencheurs séparés par des virgules).
        var env = new org.springframework.mock.env.MockEnvironment()
            .withProperty("gimac.homologation.referentiel.gestes-certification.toujours.CERTIFIEE", "a.v1, b.v1")
            .withProperty("gimac.homologation.referentiel.gestes-certification.toujours.APPROUVEE_SECURITE", "c.v1");
        assertThat(cm.gimac.des.homologation.config.DomainServicesConfigurationAcces.exigences(env, "toujours")).containsEntry(Jalon.CERTIFIEE, List.of("a.v1", "b.v1")).containsEntry(Jalon.APPROUVEE_SECURITE, List.of("c.v1"));
        assertThat(cm.gimac.des.homologation.config.DomainServicesConfigurationAcces.exigences(env, "avec-attestations")).isEmpty();
    }
}
