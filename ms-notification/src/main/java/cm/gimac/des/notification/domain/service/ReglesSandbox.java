package cm.gimac.des.notification.domain.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Règles de destinataires et gabarits des notifications de la Sandbox GIMAC (US-EXI-09, docs/lot1/sandbox-existant-adapte.md
 * §11), service de domaine pur. Chaque demande {@code sbx-<service>.notification-requested.v1} désigne un modèle ; le
 * modèle fixe le titre, le lien et la portée (institution, GIMAC ou les deux). Les destinataires sont des comptes nommés
 * de l'institution ou des rôles ({@code ROLE:ADMINISTRATEUR_INSTITUTION}, {@code ROLE:ADMINISTRATEUR_GIMAC}…), résolus
 * dans la seule portée du modèle : un compte hors de l'institution n'est jamais notifié (cloisonnement du portail).
 * Aucune donnée sensible : les gabarits ne portent que le code participant et le code de l'espace, jamais un identifiant
 * de connexion, un secret, une donnée de test ni un numéro de carte.
 */
public final class ReglesSandbox {

    /** Portée d'un modèle : comptes de l'institution, comptes du GIMAC, ou les deux. */
    public enum Portee {
        INSTITUTION,
        GIMAC,
        MIXTE,
    }

    /**
     * Modèle de notification : titre et sous-titre (gabarits sans donnée sensible), portée ; {@code intervenants} : les
     * comptes nommés sont admis hors de l'institution (intervenants d'un mandat de partenaire, désignés par SBX-ESPACE).
     */
    public record Modele(String code, String titre, String sousTitre, Portee portee, boolean intervenants) {}

    /** Destinataire résolu, avec la couche de son lien (portail pour un compte d'institution). */
    public record Destinataire(String compteId, boolean portail) {}

    /** Rôle désigné dans une demande : profil de l'institution, ou permission des comptes du GIMAC. */
    public record Role(String profilInstitution, String permissionGimac) {}

    public static final String ROLE = "ROLE:";

    /**
     * Rôles admis dans une demande ; un rôle inconnu n'est résolu en personne. EXPLOITATION et PRODUCTION_MONETIQUE :
     * forme de SBX-DONNEES (champ {@code destinataire} unique).
     */
    public static final Map<String, Role> ROLES = Map.of(
        "ADMINISTRATEUR_INSTITUTION", new Role("ROLE_SBX_ADMIN_INSTITUTION", null),
        "CHEF_DE_PROJET", new Role("ROLE_SBX_CHEF_PROJET", null),
        "DEVELOPPEUR", new Role("ROLE_SBX_DEVELOPPEUR", null),
        "TESTEUR", new Role("ROLE_SBX_TESTEUR", null),
        "ADMINISTRATEUR_GIMAC", new Role(null, "SBX_HAB_ADMINISTRATION"),
        "RSSI", new Role(null, "SBX_LEC_ADMINISTRATION"),
        "CERTIFICATEUR", new Role(null, "SBX_CER_VALIDATION"),
        "EXPLOITANT", new Role(null, "SBX_TRA_ADMINISTRATION"),
        "EXPLOITATION", new Role(null, "SBX_TRA_ADMINISTRATION"),
        "PRODUCTION_MONETIQUE", new Role(null, "SBX_DON_ADMINISTRATION")
    );

    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final Pattern COMPTE = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final Map<String, Modele> modeles = new LinkedHashMap<>();

    /**
     * Règles sur le catalogue des modèles (config/notifications-sandbox.json, lu par CatalogueNotificationsSandbox) : un
     * code en double est refusé.
     */
    public ReglesSandbox(List<Modele> catalogue) {
        for (Modele m : catalogue) {
            if (modeles.putIfAbsent(m.code(), m) != null) {
                throw new IllegalArgumentException("modèle en double au catalogue : " + m.code());
            }
        }
    }

    /** Les comptes nommés des modèles de mandat sont admis hors de l'institution (intervenants désignés par SBX-ESPACE). */
    public static boolean intervenants(String code) {
        return code.startsWith("SBX_ESP_MANDAT_") || "SBX_ESP_RAPPEL_ECHEANCE_MANDAT".equals(code);
    }

    /** Catalogue des modèles, dans l'ordre (résolution à blanc, console). */
    public List<Modele> modeles() {
        return List.copyOf(modeles.values());
    }

    public Optional<Modele> modele(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(modeles.get(code.strip()));
    }

    /** Code participant ou d'espace admis dans un gabarit : sinon remplacé par « ? » (jamais une donnée libre). */
    public static String code(String c) {
        return c != null && CODE.matcher(c.strip()).matches() ? c.strip() : "?";
    }

    public String titre(Modele m, String codeParticipant) {
        return m.titre().replace("{code}", code(codeParticipant));
    }

    /** Lien de la notification : portail de la plateforme pour l'institution, espace sandbox du DES pour le GIMAC. */
    public String lien(boolean portail, String codeParticipant) {
        String c = code(codeParticipant);
        return portail ? "/portail" : "?".equals(c) ? "/des/sandbox/espaces" : "/des/sandbox/espaces/" + c;
    }

    /**
     * Destinataires résolus dans la portée du modèle, sans doublon, dans l'ordre de la demande : un compte nommé n'est
     * retenu que s'il appartient à l'institution (ou, pour un mandat, s'il en est un intervenant désigné) ; un rôle d'institution désigne les comptes de l'institution qui portent
     * son profil ; un rôle du GIMAC, les porteurs de sa permission.
     *
     * @param comptesInstitution comptes actifs de l'institution et leurs profils
     * @param porteurs comptes du GIMAC porteurs d'une permission
     */
    public List<Destinataire> destinataires(Modele m, Collection<String> demandes, Map<String, Set<String>> comptesInstitution, Function<String, List<String>> porteurs) {
        Map<String, Destinataire> r = new LinkedHashMap<>();
        boolean institution = m.portee() != Portee.GIMAC;
        boolean gimac = m.portee() != Portee.INSTITUTION;
        for (String d : demandes == null ? List.<String>of() : demandes) {
            if (d == null || d.isBlank()) {
                continue;
            }
            String v = d.strip();
            if (!v.startsWith(ROLE)) {
                if (institution && (comptesInstitution.containsKey(v) || (m.intervenants() && COMPTE.matcher(v).matches()))) {
                    r.putIfAbsent(v, new Destinataire(v, true));
                }
                continue;
            }
            Role role = ROLES.get(v.substring(ROLE.length()));
            if (role == null) {
                continue;
            }
            if (role.profilInstitution() != null && institution) {
                comptesInstitution.forEach((id, profils) -> {
                    if (profils.contains(role.profilInstitution())) {
                        r.putIfAbsent(id, new Destinataire(id, true));
                    }
                });
            }
            if (role.permissionGimac() != null && gimac) {
                for (String id : porteurs.apply(role.permissionGimac())) {
                    r.putIfAbsent(id, new Destinataire(id, false));
                }
            }
        }
        return List.copyOf(r.values());
    }

    /** Rôles du GIMAC désignés dans la demande (pour ne lire les porteurs que s'il le faut). */
    public static Set<String> rolesDemandes(Collection<String> demandes) {
        Set<String> s = new LinkedHashSet<>();
        if (demandes != null) {
            demandes.stream().filter(d -> d != null && d.strip().startsWith(ROLE)).forEach(d -> s.add(d.strip().substring(ROLE.length())));
        }
        return s;
    }
}
