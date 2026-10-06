package cm.gimac.des.iam.domain.demande;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;

/**
 * Contenu d'une demande (§4), selon son type ; seuls les champs du type sont renseignés (les autres sont
 * nuls et omis de la représentation JSON).
 *
 * <ul>
 *   <li>CREATION_COMPTE : identifiant, prenom, nom, email, telephone (facultatif), fonction, unite,
 *       responsableId, profils ;</li>
 *   <li>PROFILS : ajouter, retirer ;</li>
 *   <li>STATUT : actif.</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContenuDemande(
    String identifiant,
    String prenom,
    String nom,
    String email,
    String telephone,
    String fonction,
    String unite,
    String responsableId,
    Set<String> profils,
    Set<String> ajouter,
    Set<String> retirer,
    Boolean actif
) {
    public static ContenuDemande creation(String identifiant, String prenom, String nom, String email, String telephone, String fonction, String unite, String responsableId, Set<String> profils) {
        return new ContenuDemande(identifiant, prenom, nom, email, telephone, fonction, unite, responsableId, profils, null, null, null);
    }

    public static ContenuDemande profils(Set<String> ajouter, Set<String> retirer) {
        return new ContenuDemande(null, null, null, null, null, null, null, null, null, ajouter, retirer, null);
    }

    public static ContenuDemande statut(boolean actif) {
        return new ContenuDemande(null, null, null, null, null, null, null, null, null, null, null, actif);
    }
}
