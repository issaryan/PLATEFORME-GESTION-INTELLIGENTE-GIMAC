package cm.gimac.des.certification.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.certification.application.port.in.Acteur;
import cm.gimac.des.certification.application.port.out.ContexteCertificationPort.ContexteCertification;
import cm.gimac.des.certification.application.port.out.ContexteCertificationPort.Couverture;
import cm.gimac.des.certification.domain.TimestampToken;
import cm.gimac.des.certification.domain.decision.ApprobationSecurite;
import cm.gimac.des.certification.domain.decision.ProcesVerbalCertification;
import cm.gimac.des.certification.domain.decision.SignatureVerrou;
import cm.gimac.des.certification.domain.enumeration.ObjetSignature;
import cm.gimac.des.certification.infrastructure.client.CachetPlateformeStubAdapter;
import cm.gimac.des.certification.infrastructure.client.ContexteCertificationStubAdapter;
import cm.gimac.des.certification.infrastructure.client.DocumentStubAdapter;
import cm.gimac.des.certification.infrastructure.client.FicheChangementStubAdapter;
import cm.gimac.des.certification.infrastructure.client.ReauthentificationStubAdapter;
import cm.gimac.des.certification.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.certification.repository.ApprobationSecuriteRepository;
import cm.gimac.des.certification.repository.ProcesVerbalCertificationRepository;
import cm.gimac.des.certification.repository.SignatureVerrouRepository;
import cm.gimac.des.certification.repository.TimestampTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Banc des tests unitaires des verrous : cas d'utilisation réels, dépôts en mémoire (Mockito), adaptateurs
 * de substitution réels, outbox simulée qui retient les événements, horloge réglable. Sans Docker.
 */
class BancDecision {

    static final String INSTITUTION = "INST-0042";
    static final String AUTRE = "INST-0017";
    static final String DOC_SHA = "a".repeat(64);

    /** Horloge réglable : les tests avancent le temps (fraîcheur de la fiche). */
    static final class Horloge extends Clock {

        Instant maintenant = Instant.parse("2026-09-30T08:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return maintenant;
        }

        void avancer(Duration d) {
            maintenant = maintenant.plus(d);
        }
    }

    record Evenement(String agregat, UUID liaisonId, String type, Object charge, String institution) {}

    final Horloge horloge = new Horloge();
    final Map<UUID, ProcesVerbalCertification> pvParLiaison = new HashMap<>();
    final Map<UUID, ApprobationSecurite> approParLiaison = new HashMap<>();
    final List<SignatureVerrou> signaturesEnregistrees = new ArrayList<>();
    final List<TimestampToken> jetonsEnregistres = new ArrayList<>();
    final List<Evenement> evenements = new ArrayList<>();

    final ProcesVerbalCertificationRepository pvs = mock(ProcesVerbalCertificationRepository.class);
    final ApprobationSecuriteRepository approbationsRepo = mock(ApprobationSecuriteRepository.class);
    final SignatureVerrouRepository signatures = mock(SignatureVerrouRepository.class);
    final TimestampTokenRepository jetons = mock(TimestampTokenRepository.class);
    final OutboxPublisher outbox = mock(OutboxPublisher.class);

    final ContexteCertificationStubAdapter contextes = new ContexteCertificationStubAdapter();
    final ReauthentificationStubAdapter reauth = new ReauthentificationStubAdapter();
    final DocumentStubAdapter documents = new DocumentStubAdapter();
    final FicheChangementStubAdapter fiches = new FicheChangementStubAdapter();
    final CachetPlateformeStubAdapter cachet = new CachetPlateformeStubAdapter("cle-de-test", horloge);

    final AccesLiaison acces = new AccesLiaison(contextes);
    final ScellementSignatures scellement = new ScellementSignatures(reauth, cachet, signatures, jetons, horloge);
    final ApprobationSecuriteService approbations;
    final ProcesVerbalService procesVerbaux;

    BancDecision() {
        this(Set.of(), Set.of());
    }

    BancDecision(Set<String> visaConformite, Set<String> visaDg) {
        when(pvs.findByLiaisonId(any())).thenAnswer(i -> Optional.ofNullable(pvParLiaison.get(i.<UUID>getArgument(0))));
        when(pvs.save(any())).thenAnswer(i -> {
            ProcesVerbalCertification p = i.getArgument(0);
            pvParLiaison.put(p.getLiaisonId(), p);
            return p;
        });
        when(approbationsRepo.findByLiaisonId(any())).thenAnswer(i -> Optional.ofNullable(approParLiaison.get(i.<UUID>getArgument(0))));
        when(approbationsRepo.save(any())).thenAnswer(i -> {
            ApprobationSecurite a = i.getArgument(0);
            approParLiaison.put(a.getLiaisonId(), a);
            return a;
        });
        when(signatures.saveAndFlush(any())).thenAnswer(i -> {
            SignatureVerrou s = i.getArgument(0);
            signaturesEnregistrees.add(s);
            return s;
        });
        when(signatures.findByLiaisonIdAndObjet(any(), any())).thenAnswer(i ->
            signaturesEnregistrees
                .stream()
                .filter(s -> s.getLiaisonId().equals(i.getArgument(0)) && s.getObjet() == i.<ObjetSignature>getArgument(1))
                .toList()
        );
        when(jetons.save(any())).thenAnswer(i -> {
            TimestampToken t = i.getArgument(0);
            t.setId(UUID.randomUUID());
            jetonsEnregistres.add(t);
            return t;
        });
        doAnswer(i -> {
            evenements.add(new Evenement(i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3), i.getArgument(4)));
            return UUID.randomUUID();
        })
            .when(outbox)
            .publish(anyString(), any(UUID.class), anyString(), any(), any());
        approbations = new ApprobationSecuriteService(
            approbationsRepo,
            pvs,
            signatures,
            acces,
            scellement,
            documents,
            fiches,
            outbox,
            horloge,
            Duration.ofMinutes(15),
            visaConformite,
            visaDg
        );
        procesVerbaux = new ProcesVerbalService(pvs, signatures, acces, scellement, approbations, outbox, horloge);
    }

    ContexteCertification liaison(String typeProjet, boolean fintech, int taux, boolean constatee) {
        return liaison(typeProjet, fintech, taux, constatee, INSTITUTION);
    }

    ContexteCertification liaison(String typeProjet, boolean fintech, int taux, boolean constatee, String institution) {
        UUID id = UUID.randomUUID();
        ContexteCertification c = new ContexteCertification(
            id,
            "LIA-2026-041",
            institution,
            "Établissement de paiement A",
            typeProjet,
            fintech,
            "EN_TEST",
            new Couverture(taux, taux / 25, 4),
            constatee,
            List.of("RETRAIT", "PAIEMENT"),
            "c-1",
            "Conducteur"
        );
        contextes.enregistrer(c);
        return c;
    }

    List<String> types() {
        return evenements.stream().map(Evenement::type).toList();
    }

    static Acteur conducteur() {
        return new Acteur("u-conducteur", "Aminata Ngono", Set.of("CER_DOSSIER_READ", "CER_PV_ESTABLISH"), null);
    }

    static Acteur chefDes() {
        return new Acteur("u-chef-des", "Chef du DES", Set.of("CER_DOSSIER_READ", "CER_QUITUS_TECHNIQUE_SIGN"), null);
    }

    static Acteur production() {
        return new Acteur("u-prod", "Production monétique", Set.of("CER_DOSSIER_READ", "CER_CHANGE_RECORD_LINK"), null);
    }

    static Acteur rssi() {
        return new Acteur("u-rssi", "RSSI", Set.of("CER_DOSSIER_READ", "CER_SECURITY_VERIFY"), null);
    }

    static Acteur conformite() {
        return new Acteur("u-conf", "Conformité", Set.of("CER_DOSSIER_READ", "CER_QUITUS_CONFORMITE_SIGN"), null);
    }

    static Acteur dg() {
        return new Acteur("u-dg", "Directeur général", Set.of("CER_DOSSIER_READ", "CER_QUITUS_DG_SIGN"), null);
    }

    static Acteur correspondant(String institution) {
        return new Acteur("u-corr", "Représentant du participant", Set.of("CER_PORTAL_READ", "CER_PORTAL_SIGN", "HOM_PORTAL_SUBMIT"), institution);
    }

    /** Procès-verbal établi et signé des deux parties : l'approbation est ouverte. */
    UUID pvSigne(String typeProjet, boolean fintech) {
        UUID id = liaison(typeProjet, fintech, 100, true).liaisonId();
        procesVerbaux.etablir(id, conducteur());
        procesVerbaux.signerGimac(id, chefDes(), "assertion");
        procesVerbaux.signerParticipant(id, correspondant(INSTITUTION), "assertion");
        return id;
    }

    static byte[] pdf() {
        return "%PDF-1.7 attestation".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }
}
