package cm.gimac.des.certification.application.usecase;

import cm.gimac.des.certification.application.port.in.Acteur;
import cm.gimac.des.certification.application.port.out.CachetPlateformePort;
import cm.gimac.des.certification.application.port.out.CachetPlateformePort.Cachet;
import cm.gimac.des.certification.application.port.out.ReauthentificationPort;
import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.domain.TimestampToken;
import cm.gimac.des.certification.domain.decision.SignatureVerrou;
import cm.gimac.des.certification.domain.decision.SignatureVerrou.Decision;
import cm.gimac.des.certification.domain.decision.SignatureVerrou.Scellement;
import cm.gimac.des.certification.domain.enumeration.ObjetSignature;
import cm.gimac.des.certification.domain.enumeration.RoleSignature;
import cm.gimac.des.certification.domain.service.DecisionCanonique;
import cm.gimac.des.certification.repository.SignatureVerrouRepository;
import cm.gimac.des.certification.repository.TimestampTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Signature d'un verrou en deux actes (D-35) : 1. assertion WebAuthn personnelle, vérifiée et consommée par
 * MS-IAM, conservée avec la décision ; 2. cachet de la plateforme sur l'empreinte de la décision canonique et
 * de l'assertion, horodaté (jeton conservé dans timestamp_token, objet SIGNATURE). La ligne enregistrée est
 * immuable.
 */
@Component
public class ScellementSignatures {

    static final String OBJET_JETON = "SIGNATURE";

    private final ReauthentificationPort reauth;
    private final CachetPlateformePort cachets;
    private final SignatureVerrouRepository signatures;
    private final TimestampTokenRepository jetons;
    private final Clock clock;

    public ScellementSignatures(
        ReauthentificationPort reauth,
        CachetPlateformePort cachets,
        SignatureVerrouRepository signatures,
        TimestampTokenRepository jetons,
        Clock clock
    ) {
        this.reauth = reauth;
        this.cachets = cachets;
        this.signatures = signatures;
        this.jetons = jetons;
        this.clock = clock;
    }

    /** 401 IAM_REAUTH_REQUIRED si l'en-tête X-Reauth-Assertion manque. */
    public static void exigerAssertion(String assertion) {
        if (assertion == null || assertion.isBlank()) {
            throw CertificationException.reauthentificationExigee();
        }
    }

    public SignatureVerrou signer(
        UUID liaisonId,
        ObjetSignature objet,
        RoleSignature role,
        Acteur acteur,
        boolean favorable,
        String motif,
        String empreinteObjet,
        String assertion
    ) {
        exigerAssertion(assertion);
        if (!reauth.verifier(assertion, acteur.id(), ReauthentificationPort.FINALITE_SIGNATURE)) {
            throw CertificationException.interdit("CER_REAUTH_INVALID", "Réauthentification forte invalide ou expirée.");
        }
        Instant le = clock.instant();
        Decision decision = new Decision(liaisonId, objet, role, acteur.id(), acteur.nom(), favorable, motif, le, empreinteObjet);
        String empreinteDecision = DecisionCanonique.de("SIGNATURE_VERROU")
            .avec("liaison", liaisonId)
            .avec("objet", objet)
            .avec("qualite", role)
            .avec("empreinteObjet", empreinteObjet)
            .avec("decision", favorable ? "FAVORABLE" : "DEFAVORABLE")
            .avec("motif", motif)
            .avec("signataire", acteur.id())
            .avec("instant", le)
            .empreinte();
        String aSceller = DecisionCanonique.sha256(empreinteDecision + "\n" + assertion);
        Cachet cachet = cachets.sceller(aSceller);
        UUID id = UUID.randomUUID();
        TimestampToken jeton = jetons.save(
            new TimestampToken()
                .objetType(OBJET_JETON)
                .objetId(id)
                .empreinte(aSceller)
                .nonce(cachet.nonce())
                .jeton(cachet.jetonHorodatage())
                .dateJeton(cachet.horodatage())
                .verifie(true)
                .autoriteChaine(cachet.autorite())
        );
        Scellement scellement = new Scellement(empreinteDecision, assertion, cachet.signature(), cachet.horodatage(), jeton.getId());
        return signatures.saveAndFlush(new SignatureVerrou(id, decision, scellement));
    }
}
