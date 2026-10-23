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

    public ReglesSandbox() {
        // SBX-ESPACE (invitations, suspension et rétablissement, mandats).
        modele("SBX_ESP_INVITATION_ADMINISTRATEUR", "Invitation : administrateur de l'espace sandbox {code}", "Votre accès est prêt sur la plateforme du GIMAC.", Portee.INSTITUTION);
        modele("SBX_ESP_INVITATION_COLLABORATEUR", "Invitation à l'espace sandbox {code}", "Votre accès est prêt sur la plateforme du GIMAC.", Portee.INSTITUTION);
        modele("SBX_ESP_ESPACE_SUSPENDU", "Espace sandbox {code} suspendu", "Les accès de l'institution à la sandbox sont suspendus.", Portee.MIXTE);
        modele("SBX_ESP_ESPACE_RETABLI", "Espace sandbox {code} rétabli", "Les accès de l'institution à la sandbox sont rétablis.", Portee.MIXTE);
        modele("SBX_ESP_RETABLISSEMENT_POSSIBLE", "Espace sandbox {code} : rétablissement possible", "Le registre des participants admet de nouveau l'institution.", Portee.GIMAC);
        modele("SBX_ESP_FERMETURE_A_PRONONCER", "Espace sandbox {code} : fermeture à prononcer", "L'institution est retirée du registre des participants.", Portee.GIMAC);
        modele("SBX_ESP_PLUS_AUCUN_ADMINISTRATEUR", "Espace sandbox {code} sans administrateur", "L'institution n'a plus d'administrateur actif.", Portee.GIMAC);
        modele("SBX_ESP_MANDAT_ACCORDE", "Mandat accordé sur l'espace sandbox {code}", "Un mandat de partenaire est accordé.", Portee.INSTITUTION);
        modele("SBX_ESP_MANDAT_ACTIF", "Mandat actif sur l'espace sandbox {code}", "Un mandat de partenaire commence.", Portee.INSTITUTION);
        modele("SBX_ESP_MANDAT_PROLONGE", "Mandat prolongé sur l'espace sandbox {code}", "La fin d'un mandat de partenaire est reportée.", Portee.INSTITUTION);
        modele("SBX_ESP_MANDAT_REVOQUE", "Mandat révoqué sur l'espace sandbox {code}", "Un mandat de partenaire est révoqué.", Portee.INSTITUTION);
        modele("SBX_ESP_MANDAT_ECHU", "Mandat échu sur l'espace sandbox {code}", "Un mandat de partenaire est arrivé à échéance.", Portee.INSTITUTION);
        modele("SBX_ESP_MANDAT_TERMINE", "Mandat terminé sur l'espace sandbox {code}", "Un mandat de partenaire est terminé.", Portee.INSTITUTION);
        modele("SBX_ESP_RAPPEL_ECHEANCE_MANDAT", "Échéance proche d'un mandat sur l'espace sandbox {code}", "Un mandat de partenaire arrive bientôt à échéance.", Portee.INSTITUTION);
        // SBX-RACCORDEMENT (échéances de certificat, collisions).
        modele("SBX_RAC_ECHEANCE_CERTIFICAT", "Certificat de test proche de son échéance ({code})", "Un certificat de raccordement à la sandbox expire bientôt.", Portee.INSTITUTION);
        modele("SBX_RAC_COLLISION_SIGNALEE", "Collision de code participant signalée ({code})", "Deux raccordements revendiquent le même code participant.", Portee.GIMAC);
        // SBX-CAMPAGNE (ouverture, affectation, anomalie, clôture).
        modele("SBX_CER_CAMPAGNE_OUVERTE", "Campagne de certification ouverte ({code})", "Une campagne est ouverte dans la sandbox.", Portee.MIXTE);
        modele("SBX_CER_CAMPAGNE_AFFECTEE", "Campagne de certification affectée ({code})", "Une campagne vous est affectée dans la sandbox.", Portee.MIXTE);
        modele("SBX_CER_ANOMALIE", "Anomalie dans une campagne de certification ({code})", "Une anomalie est constatée dans une campagne de la sandbox.", Portee.MIXTE);
        modele("SBX_CER_CAMPAGNE_CLOSE", "Campagne de certification close ({code})", "Une campagne est close dans la sandbox.", Portee.MIXTE);
        // SBX-LECTURE et SBX-TRACE (collisions, notifications abandonnées, tentatives, alertes).
        modele("SBX_LEC_COLLISION_SIGNALEE", "Collision signalée au contrat de lecture ({code})", "Une campagne est remplacée par un signalement au contrat de lecture.", Portee.GIMAC);
        modele("SBX_LEC_NOTIFICATION_ABANDONNEE", "Notification de fin de campagne abandonnée ({code})", "La plateforme n'a pas accusé la notification ; la relève périodique reprend la campagne.", Portee.GIMAC);
        modele("SBX_LEC_SEUIL_TENTATIVES_ECRITURE", "Seuil de tentatives d'écriture atteint ({code})", "Des tentatives d'écriture sur le contrat de lecture sont refusées.", Portee.GIMAC);
        modele("SBX_TRA_ALERTE", "Alerte de la sandbox ({code})", "Une alerte d'exploitation de la sandbox est levée.", Portee.GIMAC);
        // SBX-DONNEES (restauration, clés de test, plages BIN, refus hors plage).
        modele("SBX_DON_RESTAURATION_ECHEC", "Restauration d'un jeu de données en échec ({code})", "Le jeu de données est libéré ; l'essai doit être repris.", Portee.GIMAC);
        modele("SBX_DON_CLE_APPROUVEE", "Clé de test approuvée ({code})", "Une clé de test de la sandbox est approuvée.", Portee.GIMAC);
        modele("SBX_DON_CLE_REVOQUEE", "Clé de test révoquée ({code})", "Une clé de test de la sandbox est révoquée.", Portee.GIMAC);
        modele("SBX_DON_VISA_PLAGE", "Plages BIN à viser ({code})", "Un fichier de plages BIN attend son visa.", Portee.GIMAC);
        modele("SBX_DON_PLAGE_VISEE", "Plage BIN visée ({code})", "Une plage BIN de la sandbox est visée.", Portee.GIMAC);
        modele("SBX_DON_REFUS_REPETES", "Refus hors plage répétés ({code})", "Le seuil journalier de refus hors plage est dépassé.", Portee.GIMAC);
    }

    private void modele(String code, String titre, String sousTitre, Portee portee) {
        modeles.put(code, new Modele(code, titre, sousTitre, portee, code.startsWith("SBX_ESP_MANDAT_") || "SBX_ESP_RAPPEL_ECHEANCE_MANDAT".equals(code)));
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
