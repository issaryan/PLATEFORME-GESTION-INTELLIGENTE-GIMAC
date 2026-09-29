package cm.gimac.des.iam;

import com.jayway.jsonpath.JsonPath;
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
import java.util.Base64;
import java.util.List;

/**
 * Clé de sécurité FIDO2 émulée (webauthn4j-test) jouant le rôle du navigateur : répond aux options
 * d'enrôlement et aux défis de réauthentification renvoyés par MS-IAM, et sérialise l'assertion comme
 * l'interface (socle/reauth/webauthn.ts) pour l'en-tête X-Reauth-Assertion.
 */
public final class CleDeSecuriteEmulee {

    private static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder D64U = Base64.getUrlDecoder();

    private final ClientPlatform plateforme = EmulatorUtil.createClientPlatform(EmulatorUtil.NONE_ATTESTATION_AUTHENTICATOR);

    public CleDeSecuriteEmulee() {
        plateforme.setOrigin(new Origin("http://localhost"));
    }

    /** Corps JSON de POST /api/v1/moi/cles-securite à partir des options d'enrôlement. */
    public String enrolement(String optionsJson, String nom) {
        var options = new PublicKeyCredentialCreationOptions(
            new PublicKeyCredentialRpEntity(JsonPath.read(optionsJson, "$.rp.id"), JsonPath.read(optionsJson, "$.rp.name")),
            new PublicKeyCredentialUserEntity(
                D64U.decode(JsonPath.<String>read(optionsJson, "$.user.id")),
                JsonPath.read(optionsJson, "$.user.name"),
                JsonPath.read(optionsJson, "$.user.displayName")
            ),
            new DefaultChallenge(D64U.decode(JsonPath.<String>read(optionsJson, "$.challenge"))),
            List.of(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256)),
            ((Number) JsonPath.read(optionsJson, "$.timeoutMs")).longValue(),
            List.of(),
            new AuthenticatorSelectionCriteria(null, ResidentKeyRequirement.DISCOURAGED, UserVerificationRequirement.REQUIRED),
            AttestationConveyancePreference.NONE,
            null
        );
        var cred = plateforme.create(options);
        return "{\"challengeId\":\"%s\",\"nom\":\"%s\",\"clientDataJSON\":\"%s\",\"attestationObject\":\"%s\"}".formatted(
            JsonPath.<String>read(optionsJson, "$.challengeId"),
            nom,
            B64U.encodeToString(cred.getResponse().getClientDataJSON()),
            B64U.encodeToString(cred.getResponse().getAttestationObject())
        );
    }

    /** Valeur de l'en-tête X-Reauth-Assertion à partir d'un défi de réauthentification. */
    public String assertion(String defiJson) {
        List<String> autorisees = JsonPath.read(defiJson, "$.allowCredentials");
        var options = new PublicKeyCredentialRequestOptions(
            new DefaultChallenge(D64U.decode(JsonPath.<String>read(defiJson, "$.challenge"))),
            ((Number) JsonPath.read(defiJson, "$.timeoutMs")).longValue(),
            JsonPath.read(defiJson, "$.rpId"),
            autorisees.stream().map(id -> new PublicKeyCredentialDescriptor(PublicKeyCredentialType.PUBLIC_KEY, D64U.decode(id), null)).toList(),
            UserVerificationRequirement.REQUIRED,
            null
        );
        var cred = plateforme.get(options);
        var r = cred.getResponse();
        String json = "{\"challengeId\":\"%s\",\"id\":\"%s\",\"clientDataJSON\":\"%s\",\"authenticatorData\":\"%s\",\"signature\":\"%s\",\"userHandle\":%s}".formatted(
            JsonPath.<String>read(defiJson, "$.challengeId"),
            B64U.encodeToString(cred.getRawId()),
            B64U.encodeToString(r.getClientDataJSON()),
            B64U.encodeToString(r.getAuthenticatorData()),
            B64U.encodeToString(r.getSignature()),
            r.getUserHandle() == null ? "null" : "\"" + B64U.encodeToString(r.getUserHandle()) + "\""
        );
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
