package cm.gimac.des.iam.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.event.ReauthentificationPayload;
import cm.gimac.des.iam.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.iam.infrastructure.persistence.CleSecurite;
import cm.gimac.des.iam.infrastructure.persistence.CleSecuriteRepository;
import cm.gimac.des.iam.infrastructure.persistence.DefiWebAuthn;
import cm.gimac.des.iam.infrastructure.persistence.DefiWebAuthnRepository;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticatorSelectionCriteria;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.PublicKeyCredentialRpEntity;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import com.webauthn4j.data.ResidentKeyRequirement;
import com.webauthn4j.data.UserVerificationRequirement;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.test.EmulatorUtil;
import com.webauthn4j.test.client.ClientPlatform;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * Réauthentification forte FIDO2 (IAM-07) avec un authentificateur émulé : enrôlement, défi,
 * vérification, non-rejeu, sujet et finalité liés, compteur, suppression de clé.
 */
@ExtendWith(MockitoExtension.class)
class ReauthentificationServiceTest {

    static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();
    static final Base64.Decoder D64U = Base64.getUrlDecoder();
    static final Acteur CHEF = new Acteur("chef-ddrm", "Chef DDRM", Set.of());

    @Mock
    CleSecuriteRepository cles;

    @Mock
    DefiWebAuthnRepository defis;

    @Mock
    OutboxPublisher outbox;

    final Map<UUID, DefiWebAuthn> defisStockes = new HashMap<>();
    final List<CleSecurite> clesStockees = new ArrayList<>();
    Instant maintenant = Instant.parse("2026-09-29T08:00:00Z");
    ReauthentificationService service;
    ClientPlatform plateforme;

    @BeforeEach
    void init() {
        lenient().when(defis.save(any())).thenAnswer(i -> {
            DefiWebAuthn d = i.getArgument(0);
            defisStockes.put(d.getId(), d);
            return d;
        });
        lenient().when(defis.findByIdForUpdate(any())).thenAnswer(i -> Optional.ofNullable(defisStockes.get(i.<UUID>getArgument(0))));
        lenient().when(cles.save(any())).thenAnswer(i -> {
            clesStockees.add(i.getArgument(0));
            return i.getArgument(0);
        });
        lenient().when(cles.findBySujetOrderByCreeeLe(anyString())).thenAnswer(i -> clesStockees.stream().filter(c -> c.getSujet().equals(i.getArgument(0))).toList());
        lenient().when(cles.findByIdentifiantCle(anyString())).thenAnswer(i -> clesStockees.stream().filter(c -> c.getIdentifiantCle().equals(i.getArgument(0))).findFirst());
        lenient().when(cles.findById(any())).thenAnswer(i -> clesStockees.stream().filter(c -> c.getId().equals(i.getArgument(0))).findFirst());
        lenient().doAnswer(i -> clesStockees.remove(i.<CleSecurite>getArgument(0))).when(cles).delete(any());
        Clock horloge = new Clock() {
            public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(java.time.ZoneId z) { return this; }
            public Instant instant() { return maintenant; }
        };
        service = new ReauthentificationService(WebAuthnManager.createNonStrictWebAuthnManager(), cles, defis, outbox, new ObjectMapper(), AdministrationComptesServiceTest.proprietes(), horloge);
        plateforme = EmulatorUtil.createClientPlatform(EmulatorUtil.NONE_ATTESTATION_AUTHENTICATOR);
        plateforme.setOrigin(new Origin("http://localhost"));
    }

    /** Enrôle une clé émulée comme le ferait navigator.credentials.create ; renvoie son identifiant brut. */
    byte[] enroler(Acteur a) {
        var o = service.defiEnrolement(a);
        var options = new PublicKeyCredentialCreationOptions(
            new PublicKeyCredentialRpEntity(o.rp().id(), o.rp().name()),
            new PublicKeyCredentialUserEntity(D64U.decode(o.user().id()), o.user().name(), o.user().displayName()),
            new DefaultChallenge(D64U.decode(o.challenge())),
            List.of(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256)),
            o.timeoutMs(),
            List.of(),
            new AuthenticatorSelectionCriteria(null, ResidentKeyRequirement.DISCOURAGED, UserVerificationRequirement.REQUIRED),
            AttestationConveyancePreference.NONE,
            null
        );
        var cred = plateforme.create(options);
        service.enroler(a, o.challengeId(), "Clé du bureau", B64U.encodeToString(cred.getResponse().getClientDataJSON()), B64U.encodeToString(cred.getResponse().getAttestationObject()));
        return cred.getRawId();
    }

    /** Assertion sérialisée comme par l'interface (socle/reauth/webauthn.ts). */
    String assertion(Acteur a, String finalite) {
        var d = service.defi(a, finalite);
        var options = new PublicKeyCredentialRequestOptions(
            new DefaultChallenge(D64U.decode(d.challenge())),
            d.timeoutMs(),
            d.rpId(),
            d.allowCredentials().stream().map(id -> new PublicKeyCredentialDescriptor(PublicKeyCredentialType.PUBLIC_KEY, D64U.decode(id), null)).toList(),
            UserVerificationRequirement.REQUIRED,
            null
        );
        var cred = plateforme.get(options);
        var r = cred.getResponse();
        String json = "{\"challengeId\":\"%s\",\"id\":\"%s\",\"clientDataJSON\":\"%s\",\"authenticatorData\":\"%s\",\"signature\":\"%s\",\"userHandle\":%s}".formatted(
            d.challengeId(), B64U.encodeToString(cred.getRawId()), B64U.encodeToString(r.getClientDataJSON()), B64U.encodeToString(r.getAuthenticatorData()),
            B64U.encodeToString(r.getSignature()), r.getUserHandle() == null ? "null" : "\"" + B64U.encodeToString(r.getUserHandle()) + "\""
        );
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    static void refus(Runnable r, String code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo(code));
    }

    @Test
    @DisplayName("Enrôlement puis vérification d'une assertion valide ; le compteur est mis à jour")
    void nominal() {
        enroler(CHEF);
        assertThat(service.cles(CHEF)).singleElement().extracting(ReauthentificationService.Cle::nom).isEqualTo("Clé du bureau");
        assertThat(service.verifier(assertion(CHEF, "STATUS_APPROVE"), "chef-ddrm", "STATUS_APPROVE")).isTrue();
        assertThat(service.cles(CHEF).get(0).derniereUtilisation()).isEqualTo(maintenant);
        verify(outbox).publish(eq("reauthentication"), eq("chef-ddrm"), eq(ReauthentificationPayload.TYPE), eq(new ReauthentificationPayload("chef-ddrm", "STATUS_APPROVE", true, null)));
    }

    @Test
    @DisplayName("Non-rejeu : une assertion ne vaut qu'une fois ; autre sujet ou autre finalité refusés")
    void nonRejeuEtLiens() {
        enroler(CHEF);
        String a = assertion(CHEF, "STATUS_APPROVE");
        assertThat(service.verifier(a, "chef-ddrm", "STATUS_APPROVE")).isTrue();
        assertThat(service.verifier(a, "chef-ddrm", "STATUS_APPROVE")).isFalse();
        assertThat(service.verifier(assertion(CHEF, "STATUS_APPROVE"), "autre", "STATUS_APPROVE")).isFalse();
        assertThat(service.verifier(assertion(CHEF, "STATUS_APPROVE"), "chef-ddrm", "QUITUS_SIGN")).isFalse();
    }

    @Test
    @DisplayName("Défi expiré, assertion malformée ou clé d'un autre utilisateur : refus")
    void refusDivers() {
        enroler(CHEF);
        String a = assertion(CHEF, "QUITUS_SIGN");
        maintenant = maintenant.plusSeconds(600);
        assertThat(service.verifier(a, "chef-ddrm", "QUITUS_SIGN")).isFalse();
        assertThat(service.verifier(Base64.getEncoder().encodeToString("{}".getBytes(StandardCharsets.UTF_8)), "chef-ddrm", "QUITUS_SIGN")).isFalse();
        assertThat(service.verifier("%%%", "chef-ddrm", "QUITUS_SIGN")).isFalse();
        // Clé retirée entre le défi et la vérification : refus.
        String orpheline = assertion(CHEF, "QUITUS_SIGN");
        clesStockees.clear();
        assertThat(service.verifier(orpheline, "chef-ddrm", "QUITUS_SIGN")).isFalse();
    }

    @Test
    @DisplayName("D-87 : signature d'une étape de processus, finalité SIGNATURE_PROCESSUS liée au défi ; autre finalité refusée ; audit publié")
    void signatureProcessus() {
        enroler(CHEF);
        assertThat(service.verifier(assertion(CHEF, "SIGNATURE_PROCESSUS"), "chef-ddrm", "SIGNATURE_PROCESSUS")).isTrue();
        verify(outbox).publish(eq("reauthentication"), eq("chef-ddrm"), eq(ReauthentificationPayload.TYPE), eq(new ReauthentificationPayload("chef-ddrm", "SIGNATURE_PROCESSUS", true, null)));
        assertThat(service.verifier(assertion(CHEF, "SIGNATURE_PROCESSUS"), "chef-ddrm", "STATUS_APPROVE")).isFalse();
        assertThat(service.verifier(assertion(CHEF, "STATUS_APPROVE"), "chef-ddrm", "SIGNATURE_PROCESSUS")).isFalse();
    }

    @Test
    @DisplayName("Défi refusé sans clé enrôlée ou avec une finalité invalide ; enrôlement invalide refusé")
    void preconditions() {
        refus(() -> service.defi(CHEF, "STATUS_APPROVE"), "IAM_NO_SECURITY_KEY");
        refus(() -> service.defi(CHEF, "minuscules"), "IAM_PURPOSE_INVALID");
        refus(() -> service.defi(CHEF, null), "IAM_PURPOSE_INVALID");
        var o = service.defiEnrolement(CHEF);
        refus(() -> service.enroler(CHEF, o.challengeId(), " ", "e30", "e30"), "IAM_KEY_NAME_INVALID");
        refus(() -> service.enroler(CHEF, o.challengeId(), "Clé", "e30", "e30"), "IAM_KEY_REJECTED");
        refus(() -> service.enroler(CHEF, o.challengeId(), "Clé", "e30", "e30"), "IAM_CHALLENGE_INVALID");
        refus(() -> service.enroler(CHEF, "pas-un-uuid", "Clé", "e30", "e30"), "IAM_CHALLENGE_INVALID");
        refus(() -> service.enroler(CHEF, UUID.randomUUID().toString(), "Clé", "e30", "e30"), "IAM_CHALLENGE_INVALID");
    }

    @Test
    @DisplayName("Une clé ne s'enrôle qu'une fois ; suppression par son titulaire seulement")
    void cycleDeLaCle() {
        enroler(CHEF);
        assertThat(service.defiEnrolement(CHEF).excludeCredentials()).hasSize(1);
        UUID id = service.cles(CHEF).get(0).id();
        refus(() -> service.supprimerCle(new Acteur("autre", "x", Set.of()), id), "IAM_KEY_NOT_FOUND");
        service.supprimerCle(CHEF, id);
        assertThat(service.cles(CHEF)).isEmpty();
    }
}
