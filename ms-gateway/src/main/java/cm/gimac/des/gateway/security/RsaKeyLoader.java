package cm.gimac.des.gateway.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Chargement de la paire RSA servant a signer les JWT internes (RS256).
 *
 * <p>
 * Classe pure : aucune dependance a Spring, aucun effet de bord. Elle est
 * appelee par {@link RsaKeyConfig} qui, lui, porte la logique de resolution
 * (coffre-fort, puis fichier, puis generation ephemere).
 *
 * <p>
 * <b>Identifiant de cle deterministe.</b> Le {@code kid} n'est pas tire au sort :
 * il est l'empreinte RFC 7638 de la cle publique. Deux instances de passerelle
 * chargeant le meme PEM annoncent donc le meme {@code kid}, et un redemarrage
 * ne renomme pas la cle. C'est la condition pour qu'un jeton emis par une
 * instance soit accepte par une autre, et pour qu'une rotation soit lisible.
 *
 * <p>
 * <b>Format attendu.</b> Cle privee au format PKCS#8 non chiffre
 * ({@code -----BEGIN PRIVATE KEY-----}), cle publique au format SPKI
 * ({@code -----BEGIN PUBLIC KEY-----}). Le format PKCS#1
 * ({@code -----BEGIN RSA PRIVATE KEY-----}) est explicitement rejete avec la
 * commande de conversion, plutot que de produire une erreur de decodage opaque.
 */
public final class RsaKeyLoader {

    private static final String PKCS8_DEBUT = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS1_DEBUT = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String SPKI_DEBUT = "-----BEGIN PUBLIC KEY-----";

    private RsaKeyLoader() {
        // classe utilitaire
    }

    /**
     * Construit la paire de signature a partir d'un PEM PKCS#8 en memoire.
     *
     * <p>
     * C'est la forme que prend un secret servi par un coffre-fort : une chaine,
     * jamais un fichier. La meme methode sert donc au chemin Vault et au chemin
     * fichier, ce dernier n'etant qu'une lecture prealable.
     *
     * @param pemPrive        contenu PEM de la cle privee PKCS#8
     * @param tailleMinimale  taille minimale acceptee, en bits
     * @return la cle JWK complete (privee + publique), {@code kid} deterministe
     * @throws GeneralSecurityException si le PEM est illisible ou la cle trop courte
     */
    public static RSAKey depuisPemPrive(String pemPrive, int tailleMinimale) throws GeneralSecurityException {
        String pem = pemPrive == null ? "" : pemPrive.trim();
        if (pem.contains(PKCS1_DEBUT)) {
            throw new GeneralSecurityException(
                    "Cle privee au format PKCS#1 non supportee. Convertir en PKCS#8 : " +
                    "openssl pkcs8 -topk8 -nocrypt -in cle.pem -out cle-pkcs8.pem");
        }
        if (!pem.contains(PKCS8_DEBUT)) {
            throw new GeneralSecurityException(
                    "Cle privee illisible : en-tete '" + PKCS8_DEBUT + "' absent.");
        }
        byte[] der = decoderCorpsPem(pem);
        PrivateKey privee = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        if (!(privee instanceof RSAPrivateCrtKey crt)) {
            throw new GeneralSecurityException(
                    "La cle privee ne porte pas les parametres CRT : la cle publique " +
                    "ne peut pas en etre derivee. Regenerer avec 'openssl genrsa'.");
        }
        RSAPublicKey publique = (RSAPublicKey) KeyFactory
                .getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
        verifierTaille(publique, tailleMinimale);
        return construire(publique, crt);
    }

    /**
     * Lit un PEM PKCS#8 depuis un volume monte.
     *
     * @param chemin          emplacement du fichier PEM
     * @param tailleMinimale  taille minimale acceptee, en bits
     * @return la cle JWK complete
     * @throws GeneralSecurityException si le fichier est illisible ou la cle invalide
     */
    public static RSAKey depuisFichier(Path chemin, int tailleMinimale) throws GeneralSecurityException {
        String contenu;
        try {
            contenu = Files.readString(chemin, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GeneralSecurityException("Cle de signature illisible : " + chemin, e);
        }
        return depuisPemPrive(contenu, tailleMinimale);
    }

    /**
     * Lit une cle publique SPKI — utilisee pour publier au JWKS les cles
     * anciennes encore valides pendant une rotation.
     *
     * @param chemin emplacement du fichier PEM public
     * @return la cle JWK publique seule, {@code kid} deterministe
     * @throws GeneralSecurityException si le fichier est illisible ou la cle invalide
     */
    public static RSAKey publiqueDepuisFichier(Path chemin) throws GeneralSecurityException {
        String pem;
        try {
            pem = Files.readString(chemin, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new GeneralSecurityException("Cle publique illisible : " + chemin, e);
        }
        if (!pem.contains(SPKI_DEBUT)) {
            throw new GeneralSecurityException(
                    "Cle publique illisible : en-tete '" + SPKI_DEBUT + "' absent (" + chemin + ").");
        }
        PublicKey publique = KeyFactory
                .getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(decoderCorpsPem(pem)));
        return construire((RSAPublicKey) publique, null);
    }

    /**
     * Genere une paire ephemere. Reserve au developpement : la cle disparait a
     * l'arret du processus, donc tous les jetons emis deviennent invalides.
     *
     * @param taille taille de la cle, en bits
     * @return la cle JWK complete
     * @throws GeneralSecurityException si le generateur refuse la taille demandee
     */
    public static RSAKey genererEphemere(int taille) throws GeneralSecurityException {
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(taille);
        KeyPair paire = generateur.generateKeyPair();
        return construire((RSAPublicKey) paire.getPublic(), (RSAPrivateKey) paire.getPrivate());
    }

    /** Assemble la JWK et derive le {@code kid} de l'empreinte RFC 7638. */
    private static RSAKey construire(RSAPublicKey publique, RSAPrivateKey privee) throws GeneralSecurityException {
        RSAKey.Builder builder = new RSAKey.Builder(publique)
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE);
        if (privee != null) {
            builder.privateKey(privee);
        }
        RSAKey sansKid = builder.build();
        try {
            return new RSAKey.Builder(sansKid).keyID(sansKid.computeThumbprint().toString()).build();
        } catch (JOSEException e) {
            throw new GeneralSecurityException("Empreinte RFC 7638 incalculable", e);
        }
    }

    private static void verifierTaille(RSAPublicKey publique, int tailleMinimale) throws GeneralSecurityException {
        int bits = publique.getModulus().bitLength();
        if (bits < tailleMinimale) {
            throw new GeneralSecurityException(
                    "Cle de signature de " + bits + " bits : le minimum exige est " + tailleMinimale + " bits.");
        }
    }

    private static byte[] decoderCorpsPem(String pem) throws GeneralSecurityException {
        String corps = pem.replaceAll("-----(BEGIN|END)[^-]+-----", "").replaceAll("\\s", "");
        try {
            return Base64.getDecoder().decode(corps);
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Corps PEM non decodable en Base64", e);
        }
    }
}
