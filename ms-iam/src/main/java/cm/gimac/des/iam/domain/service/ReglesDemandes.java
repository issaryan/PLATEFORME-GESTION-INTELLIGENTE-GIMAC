package cm.gimac.des.iam.domain.service;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.demande.ContenuDemande;
import cm.gimac.des.iam.domain.demande.EtatDemande;
import cm.gimac.des.iam.domain.demande.TypeDemande;
import cm.gimac.des.iam.domain.pouvoir.ActionPouvoir;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Règles des demandes à l'administration (D-81, docs/lot1/pouvoirs-hierarchiques.md §4), service de
 * domaine pur : forme du contenu selon le type, motif de 5 à 500 caractères, clé d'unicité (une seule
 * demande EN_ATTENTE par cible et par type), action de la matrice et permission de l'acte de chaque type.
 */
public final class ReglesDemandes {

    public static final int MOTIF_MIN = 5;
    public static final int MOTIF_MAX = 500;
    private static final Pattern COURRIEL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private ReglesDemandes() {}

    /** Action de la matrice qui ouvre ce type de demande. */
    public static ActionPouvoir action(TypeDemande type) {
        return switch (type) {
            case CREATION_COMPTE -> ActionPouvoir.COMPTE_DEMANDER;
            case PROFILS -> ActionPouvoir.PROFIL_DEMANDER;
            case STATUT -> ActionPouvoir.STATUT_DEMANDER;
        };
    }

    /** Permission de l'acte direct, exigée pour accepter la demande. */
    public static String permissionActe(TypeDemande type) {
        return switch (type) {
            case CREATION_COMPTE -> "IAM_ACCOUNT_CREATE";
            case PROFILS -> "IAM_ACCOUNT_ROLES";
            case STATUT -> "IAM_ACCOUNT_STATUS";
        };
    }

    /** Motif obligatoire, 5 à 500 caractères (rogné), sinon 400 ORG_DEMANDE_INVALIDE. */
    public static String motif(String motif, String champ) {
        String m = motif == null ? "" : motif.trim();
        if (m.length() < MOTIF_MIN || m.length() > MOTIF_MAX) {
            throw invalide("Motif obligatoire, de " + MOTIF_MIN + " à " + MOTIF_MAX + " caractères", champ);
        }
        return m;
    }

    /**
     * Contrôle et normalise le contenu selon le type ; la cible est exigée pour PROFILS et STATUT,
     * absente pour une création.
     */
    public static ContenuDemande normaliser(TypeDemande type, String cibleId, ContenuDemande c) {
        if (type == null) {
            throw invalide("Type de demande obligatoire", "type");
        }
        if (c == null) {
            throw invalide("Contenu obligatoire", "contenu");
        }
        return switch (type) {
            case CREATION_COMPTE -> creation(cibleId, c);
            case PROFILS -> profils(cibleId, c);
            case STATUT -> statut(cibleId, c);
        };
    }

    private static ContenuDemande creation(String cibleId, ContenuDemande c) {
        if (cibleId != null) {
            throw invalide("Une création de compte n'a pas de cible", "cibleId");
        }
        String identifiant = c.identifiant() == null ? null : c.identifiant().trim().toLowerCase(Locale.ROOT);
        ReglesAdministration.verifierIdentifiant(identifiant);
        ReglesAdministration.verifierTelephone(c.telephone());
        String email = c.email() == null ? null : c.email().trim();
        if (email == null || email.length() > 255 || !COURRIEL.matcher(email).matches()) {
            throw invalide("Courriel invalide", "email");
        }
        String prenom = texte(c.prenom(), "prenom", 100);
        String nom = texte(c.nom(), "nom", 100);
        String fonction = texte(c.fonction(), "fonction", 200);
        String unite = texte(c.unite(), "unite", 32);
        String responsable = texte(c.responsableId(), "responsableId", 36);
        if (c.profils() == null || c.profils().isEmpty()) {
            throw invalide("Au moins un profil est proposé", "profils");
        }
        String telephone = c.telephone() == null || c.telephone().isBlank() ? null : c.telephone().trim();
        return ContenuDemande.creation(identifiant, prenom, nom, email, telephone, fonction, unite, responsable, new TreeSet<>(c.profils()));
    }

    private static ContenuDemande profils(String cibleId, ContenuDemande c) {
        exigerCible(cibleId);
        Set<String> ajouter = c.ajouter() == null ? Set.of() : new TreeSet<>(c.ajouter());
        Set<String> retirer = c.retirer() == null ? Set.of() : new TreeSet<>(c.retirer());
        if (ajouter.isEmpty() && retirer.isEmpty()) {
            throw invalide("Au moins un profil à ajouter ou à retirer", "ajouter");
        }
        Set<String> communs = new HashSet<>(ajouter);
        communs.retainAll(retirer);
        if (!communs.isEmpty()) {
            throw invalide("Un profil ne peut être à la fois ajouté et retiré", "retirer");
        }
        return ContenuDemande.profils(ajouter, retirer);
    }

    private static ContenuDemande statut(String cibleId, ContenuDemande c) {
        exigerCible(cibleId);
        if (c.actif() == null) {
            throw invalide("État demandé obligatoire", "actif");
        }
        return ContenuDemande.statut(c.actif());
    }

    /** Profils proposés par la demande : à attribuer à la création, à ajouter ou à retirer. */
    public static Set<String> profilsProposes(ContenuDemande c) {
        Set<String> s = new TreeSet<>();
        if (c.profils() != null) s.addAll(c.profils());
        if (c.ajouter() != null) s.addAll(c.ajouter());
        if (c.retirer() != null) s.addAll(c.retirer());
        return s;
    }

    /** Clé d'unicité : le compte visé, ou l'identifiant proposé pour une création. */
    public static String cleCible(TypeDemande type, String cibleId, ContenuDemande c) {
        return type == TypeDemande.CREATION_COMPTE ? "identifiant:" + c.identifiant() : cibleId;
    }

    /** Profils du compte après l'acte : actuels, plus les ajouts, moins les retraits. */
    public static Set<String> profilsApres(Set<String> actuels, ContenuDemande c) {
        Set<String> s = new TreeSet<>(actuels);
        if (c.ajouter() != null) s.addAll(c.ajouter());
        if (c.retirer() != null) s.removeAll(c.retirer());
        return s;
    }

    /** Une demande déjà décidée ou annulée ne change plus : 409 ORG_DEMANDE_TRAITEE. */
    public static void exigerEnAttente(EtatDemande etat) {
        if (etat != EtatDemande.EN_ATTENTE) {
            throw IamException.conflit("ORG_DEMANDE_TRAITEE", "La demande n'est plus en attente", Map.of("etat", etat.name()));
        }
    }

    private static void exigerCible(String cibleId) {
        if (cibleId == null || cibleId.isBlank()) {
            throw invalide("Compte visé obligatoire", "cibleId");
        }
    }

    private static String texte(String v, String champ, int max) {
        String t = v == null ? "" : v.trim();
        if (t.isEmpty() || t.length() > max) {
            throw invalide("Champ obligatoire, " + max + " caractères au plus", champ);
        }
        return t;
    }

    private static IamException invalide(String message, String champ) {
        return IamException.invalide("ORG_DEMANDE_INVALIDE", message, Map.of("champ", champ));
    }
}
