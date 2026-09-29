package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.IamProprietes;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.event.ReauthentificationPayload;
import cm.gimac.des.iam.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.iam.infrastructure.persistence.CleSecurite;
import cm.gimac.des.iam.infrastructure.persistence.CleSecuriteRepository;
import cm.gimac.des.iam.infrastructure.persistence.DefiWebAuthn;
import cm.gimac.des.iam.infrastructure.persistence.DefiWebAuthnRepository;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Réauthentification forte FIDO2 (IAM-07) : enrôlement des clés de sécurité depuis « Mon compte »,
 * défi à usage unique lié au sujet et à la finalité, vérification de l'assertion pour le service
 * qui porte la décision (maker-checker, quitus, clôture). L'assertion n'est jamais rejouable :
 * le défi est consommé à la première vérification.
 */
@Service
@Transactional
public class ReauthentificationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReauthentificationService.class);
    private static final Pattern FINALITE = Pattern.compile("^[A-Z][A-Z_]{2,59}$");
    private static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder D64U = Base64.getUrlDecoder();

    public record Rp(String id, String name) {}

    public record Utilisateur(String id, String name, String displayName) {}

    public record Algorithme(String type, int alg) {}

    /** Options d'enrôlement transmises à navigator.credentials.create. */
    public record OptionsEnrolement(String challengeId, String challenge, Rp rp, Utilisateur user, List<Algorithme> pubKeyCredParams, long timeoutMs, List<String> excludeCredentials) {}

    /** Défi de réauthentification transmis à navigator.credentials.get. */
    public record Defi(String challengeId, String challenge, String rpId, List<String> allowCredentials, long timeoutMs) {}

    public record Cle(UUID id, String nom, Instant creeeLe, Instant derniereUtilisation) {
        static Cle de(CleSecurite c) {
            return new Cle(c.getId(), c.getNom(), c.getCreeeLe(), c.getDerniereUtilisation());
        }
    }

    private final WebAuthnManager webauthn;
    private final AttestedCredentialDataConverter convertisseur = new AttestedCredentialDataConverter(new ObjectConverter());
    private final CleSecuriteRepository cles;
    private final DefiWebAuthnRepository defis;
    private final OutboxPublisher outbox;
    private final ObjectMapper json;
    private final IamProprietes.Webauthn config;
    private final Clock clock;
    private final SecureRandom aleatoire = new SecureRandom();

    public ReauthentificationService(
        WebAuthnManager webauthn,
        CleSecuriteRepository cles,
        DefiWebAuthnRepository defis,
        OutboxPublisher outbox,
        ObjectMapper json,
        IamProprietes proprietes,
        Clock clock
    ) {
        this.webauthn = webauthn;
        this.cles = cles;
        this.defis = defis;
        this.outbox = outbox;
        this.json = json;
        this.config = proprietes.webauthn();
        this.clock = clock;
    }

    // ------------------------------------------------------------------ enrôlement

    public OptionsEnrolement defiEnrolement(Acteur acteur) {
        DefiWebAuthn d = nouveauDefi(acteur.id(), DefiWebAuthn.ENROLEMENT);
        List<String> exclues = cles.findBySujetOrderByCreeeLe(acteur.id()).stream().map(CleSecurite::getIdentifiantCle).toList();
        return new OptionsEnrolement(
            d.getId().toString(),
            d.getDefi(),
            new Rp(config.rpId(), config.rpNom()),
            new Utilisateur(B64U.encodeToString(acteur.id().getBytes(StandardCharsets.UTF_8)), acteur.id(), acteur.nom()),
            List.of(new Algorithme("public-key", -7), new Algorithme("public-key", -257)),
            config.validiteDefi().toMillis(),
            exclues
        );
    }

    public Cle enroler(Acteur acteur, String challengeId, String nom, String clientDataJSON, String attestationObject) {
        if (nom == null || nom.isBlank() || nom.length() > 100) {
            throw IamException.invalide("IAM_KEY_NAME_INVALID", "Nom de la clé obligatoire (100 caractères au plus)", Map.of());
        }
        DefiWebAuthn d = consommer(challengeId, acteur.id(), DefiWebAuthn.ENROLEMENT);
        try {
            RegistrationData data = webauthn.verify(
                new RegistrationRequest(decoder(attestationObject), decoder(clientDataJSON)),
                new RegistrationParameters(
                    serveur(d),
                    List.of(
                        new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
                        new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256)
                    ),
                    true,
                    true
                )
            );
            var authData = data.getAttestationObject().getAuthenticatorData();
            AttestedCredentialData attestees = authData.getAttestedCredentialData();
            String identifiant = B64U.encodeToString(attestees.getCredentialId());
            if (cles.findByIdentifiantCle(identifiant).isPresent()) {
                throw IamException.conflit("IAM_KEY_ALREADY_ENROLLED", "Cette clé est déjà enrôlée", Map.of());
            }
            CleSecurite cle = cles.save(new CleSecurite(acteur.id(), nom.trim(), identifiant, B64U.encodeToString(convertisseur.convert(attestees)), authData.getSignCount(), clock.instant()));
            return Cle.de(cle);
        } catch (IamException e) {
            throw e;
        } catch (RuntimeException e) {
            LOG.warn("Enrôlement FIDO2 refusé pour {} : {}", acteur.id(), e.getMessage());
            throw IamException.invalide("IAM_KEY_REJECTED", "Enrôlement de la clé refusé", Map.of());
        }
    }

    @Transactional(readOnly = true)
    public List<Cle> cles(Acteur acteur) {
        return cles.findBySujetOrderByCreeeLe(acteur.id()).stream().map(Cle::de).toList();
    }

    public void supprimerCle(Acteur acteur, UUID id) {
        CleSecurite c = cles.findById(id).filter(x -> x.getSujet().equals(acteur.id())).orElseThrow(() -> IamException.introuvable("IAM_KEY_NOT_FOUND", id));
        cles.delete(c);
    }

    // ------------------------------------------------------------------ réauthentification

    public Defi defi(Acteur acteur, String finalite) {
        if (finalite == null || !FINALITE.matcher(finalite).matches()) {
            throw IamException.invalide("IAM_PURPOSE_INVALID", "Finalité invalide", Map.of());
        }
        List<String> autorisees = cles.findBySujetOrderByCreeeLe(acteur.id()).stream().map(CleSecurite::getIdentifiantCle).toList();
        if (autorisees.isEmpty()) {
            throw IamException.conflit("IAM_NO_SECURITY_KEY", "Aucune clé de sécurité enrôlée : enrôlez-en une dans « Mon compte »", Map.of());
        }
        DefiWebAuthn d = nouveauDefi(acteur.id(), finalite);
        return new Defi(d.getId().toString(), d.getDefi(), config.rpId(), autorisees, config.validiteDefi().toMillis());
    }

    /**
     * Vérifie une assertion (en-tête X-Reauth-Assertion) pour le sujet et la finalité annoncés par
     * le service appelant. Tout écart (défi inconnu, expiré, déjà consommé, autre sujet, autre
     * finalité, signature invalide, compteur régressif) donne faux. Le résultat est publié vers
     * l'audit dans une transaction propre, même en cas de refus.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = IamException.class)
    public boolean verifier(String assertionB64, String sujet, String finalite) {
        String refus = null;
        try {
            JsonNode a = json.readTree(new String(Base64.getDecoder().decode(assertionB64), StandardCharsets.UTF_8));
            DefiWebAuthn d = consommer(a.path("challengeId").asString(), sujet, finalite);
            String identifiant = a.path("id").asString();
            CleSecurite cle = cles.findByIdentifiantCle(identifiant).filter(c -> c.getSujet().equals(sujet)).orElse(null);
            if (cle == null) {
                refus = "cle inconnue";
            } else {
                AttestedCredentialData attestees = convertisseur.convert(decoder(cle.getDonneesAttestees()));
                var enregistrement = new CredentialRecordImpl(new NoneAttestationStatement(), null, null, null, cle.getCompteur(), attestees, null, null, null, null);
                AuthenticationData resultat = webauthn.verify(
                    new AuthenticationRequest(
                        decoder(identifiant),
                        a.path("userHandle").isNull() || a.path("userHandle").isMissingNode() ? null : decoder(a.path("userHandle").asString()),
                        decoder(a.path("authenticatorData").asString()),
                        decoder(a.path("clientDataJSON").asString()),
                        decoder(a.path("signature").asString())
                    ),
                    new AuthenticationParameters(serveur(d), enregistrement, List.of(decoder(identifiant)), true, true)
                );
                cle.utilisee(resultat.getAuthenticatorData().getSignCount(), clock.instant());
            }
        } catch (IamException e) {
            refus = e.code();
        } catch (RuntimeException e) {
            refus = "assertion invalide";
            LOG.warn("Réauthentification refusée pour {} ({}) : {}", sujet, finalite, e.getMessage());
        }
        outbox.publish("reauthentication", sujet, ReauthentificationPayload.TYPE, new ReauthentificationPayload(sujet, finalite, refus == null, refus));
        return refus == null;
    }

    // ------------------------------------------------------------------ outils

    private DefiWebAuthn nouveauDefi(String sujet, String finalite) {
        byte[] octets = new byte[32];
        aleatoire.nextBytes(octets);
        return defis.save(new DefiWebAuthn(sujet, finalite, B64U.encodeToString(octets), clock.instant().plus(config.validiteDefi())));
    }

    private DefiWebAuthn consommer(String challengeId, String sujet, String finalite) {
        UUID id;
        try {
            id = UUID.fromString(challengeId);
        } catch (RuntimeException e) {
            throw IamException.invalide("IAM_CHALLENGE_INVALID", "Défi inconnu", Map.of());
        }
        DefiWebAuthn d = defis.findByIdForUpdate(id).orElseThrow(() -> IamException.invalide("IAM_CHALLENGE_INVALID", "Défi inconnu", Map.of()));
        boolean utilisable = d.utilisable(clock.instant()) && d.getSujet().equals(sujet) && d.getFinalite().equals(finalite);
        d.consommer();
        if (!utilisable) {
            throw IamException.invalide("IAM_CHALLENGE_INVALID", "Défi expiré, déjà utilisé ou étranger à cette demande", Map.of());
        }
        return d;
    }

    private ServerProperty serveur(DefiWebAuthn d) {
        Set<Origin> origines = config.origines().stream().map(Origin::new).collect(Collectors.toSet());
        return new ServerProperty(origines, config.rpId(), new DefaultChallenge(decoder(d.getDefi())));
    }

    private static byte[] decoder(String b64u) {
        if (b64u == null) {
            throw IamException.invalide("IAM_CHALLENGE_INVALID", "Donnée manquante", Map.of());
        }
        return D64U.decode(b64u);
    }
}
