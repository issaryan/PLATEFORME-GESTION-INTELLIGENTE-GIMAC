package cm.gimac.des.gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import cm.gimac.des.gateway.config.ApplicationProperties;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fournit la paire RSA-2048 qui signe les JWT internes (RS256) et le jeu de
 * cles publiques publie par {@code /api/internal/jwks}.
 *
 * <p>
 * <b>Ordre de resolution</b>, du plus sur au moins sur :
 * <ol>
 * <li>{@code application.signing-key.private-key-pem} — le secret tel que le
 * sert un coffre-fort : une chaine en memoire, jamais un fichier sur disque ;</li>
 * <li>{@code application.signing-key.private-key-path} — un PEM PKCS#8 sur un
 * volume monte, pour les deploiements sans coffre-fort ;</li>
 * <li>generation ephemere — <b>developpement uniquement</b>, et seulement si
 * {@code application.signing-key.allow-ephemeral} vaut vrai.</li>
 * </ol>
 *
 * <p>
 * <b>Pourquoi le troisieme cas doit rester bruyant.</b> Une paire ephemere se
 * comporte normalement jusqu'au premier redemarrage : les jetons deja emis
 * deviennent alors invalides sans qu'aucune erreur n'ait ete levee, et derriere
 * un repartiteur a deux instances, une requete sur deux echoue selon l'instance
 * qui a signe. La panne est intermittente et ne pointe vers rien. Le mode est
 * donc annonce en {@code WARN} a chaque demarrage, et interdit lorsque
 * {@code allow-ephemeral} est faux — le service refuse de demarrer plutot que
 * de basculer en silence sur une cle jetable.
 *
 * <p>
 * <b>Rotation.</b> Le {@code kid} etant l'empreinte RFC 7638 de la cle publique,
 * il change avec la cle. Pour tourner sans coupure : deposer la nouvelle paire
 * en {@code private-key-*}, ajouter l'ancienne cle <i>publique</i> a
 * {@code previous-public-key-paths}, redemarrer, puis retirer l'ancienne une
 * fois passee la duree de vie du plus long jeton emis. Pendant la transition, le
 * JWKS publie les deux cles : les jetons anciens restent verifiables, les
 * nouveaux sont signes avec la nouvelle. La procedure detaillee est en
 * {@code api-gateway/docs/ROTATION_CLE_SIGNATURE.md}.
 */
@ConditionalOnProperty(name = "gimac.securite.jeton-fantome.actif", havingValue = "true")
@Configuration
public class RsaKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyConfig.class);

    /**
     * Resout la paire de signature selon l'ordre documente ci-dessus.
     *
     * @param proprietes configuration validee de la passerelle
     * @return la paire RSA complete, {@code kid} deterministe
     * @throws GeneralSecurityException si la source est illisible, ou si aucune
     *                                  source persistante n'est fournie alors que
     *                                  la generation ephemere est interdite
     */
    @Bean
    public RSAKey internalRsaKey(ApplicationProperties proprietes) throws GeneralSecurityException {
        ApplicationProperties.SigningKey conf = proprietes.getSigningKey();

        if (conf.privateKeyPem() != null && !conf.privateKeyPem().isBlank()) {
            RSAKey cle = RsaKeyLoader.depuisPemPrive(conf.privateKeyPem(), conf.minimumKeySize());
            log.info("Cle de signature interne chargee depuis le coffre-fort (kid={}).", cle.getKeyID());
            return cle;
        }

        if (conf.privateKeyPath() != null && !conf.privateKeyPath().isBlank()) {
            Path chemin = Path.of(conf.privateKeyPath());
            if (!Files.isReadable(chemin)) {
                throw new GeneralSecurityException(
                        "Cle de signature introuvable ou illisible : " + chemin +
                        ". Verifier le montage du volume et les droits du fichier.");
            }
            RSAKey cle = RsaKeyLoader.depuisFichier(chemin, conf.minimumKeySize());
            log.info("Cle de signature interne chargee depuis {} (kid={}).", chemin, cle.getKeyID());
            return cle;
        }

        if (!conf.allowEphemeral()) {
            throw new GeneralSecurityException(
                    "Aucune cle de signature persistante fournie et la generation ephemere est " +
                    "interdite (application.signing-key.allow-ephemeral=false). Renseigner " +
                    "application.signing-key.private-key-pem (coffre-fort) ou " +
                    "application.signing-key.private-key-path (volume monte).");
        }

        RSAKey cle = RsaKeyLoader.genererEphemere(conf.minimumKeySize());
        log.warn(
                "DEVELOPPEMENT UNIQUEMENT — cle de signature ephemere generee (kid={}). " +
                "Les jetons emis seront invalides apres un redemarrage, et deux instances " +
                "de passerelle n'accepteront pas les jetons l'une de l'autre. Fournir une " +
                "cle persistante avant tout deploiement.",
                cle.getKeyID());
        return cle;
    }

    /**
     * Assemble le jeu publie par {@code /api/internal/jwks} : la cle courante,
     * suivie des cles publiques encore en periode de recouvrement.
     *
     * <p>
     * Une entree de {@code previous-public-key-paths} illisible fait echouer le
     * demarrage : la publier partiellement reviendrait a rejeter des jetons
     * encore valides, ce que la rotation cherche precisement a eviter. Un
     * doublon (meme empreinte que la cle courante) est ignore sans erreur, car
     * il decrit une transition deja terminee.
     *
     * @param internalRsaKey la paire courante
     * @param proprietes     configuration validee de la passerelle
     * @return le jeu de cles publiques, cle courante en tete
     * @throws GeneralSecurityException si une cle precedente est illisible
     */
    @Bean
    public JWKSet internalJwkSet(RSAKey internalRsaKey, ApplicationProperties proprietes)
            throws GeneralSecurityException {
        Map<String, JWK> parKid = new LinkedHashMap<>();
        parKid.put(internalRsaKey.getKeyID(), internalRsaKey.toPublicJWK());

        // La liste est @NotNull dans ApplicationProperties, mais ce bean est aussi
        // appelable hors du cycle de validation (tests, configuration construite a la
        // main). Une NullPointerException au demarrage y serait illisible.
        List<String> precedentes = proprietes.getSigningKey().previousPublicKeyPaths();
        if (precedentes == null) {
            precedentes = List.of();
        }
        List<String> ignorees = new ArrayList<>();
        for (String chemin : precedentes) {
            if (chemin == null || chemin.isBlank()) {
                continue;
            }
            RSAKey ancienne = RsaKeyLoader.publiqueDepuisFichier(Path.of(chemin));
            if (parKid.putIfAbsent(ancienne.getKeyID(), ancienne) != null) {
                ignorees.add(chemin);
            }
        }

        if (!ignorees.isEmpty()) {
            log.warn(
                    "Cles precedentes ignorees car identiques a la cle courante : {}. " +
                    "La rotation qu'elles decrivent est terminee, elles peuvent etre retirees " +
                    "de application.signing-key.previous-public-key-paths.",
                    ignorees);
        }
        if (parKid.size() > 1) {
            log.info("Rotation en cours — {} cles publiees au JWKS : {}.", parKid.size(), parKid.keySet());
        }
        return new JWKSet(new ArrayList<>(parKid.values()));
    }
}
