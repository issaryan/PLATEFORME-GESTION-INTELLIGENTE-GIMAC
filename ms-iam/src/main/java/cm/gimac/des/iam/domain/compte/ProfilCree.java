package cm.gimac.des.iam.domain.compte;

import java.time.Instant;

/**
 * Profil créé par l'administration (D-81, docs/lot1/pouvoirs-hierarchiques.md §5), tenu par MS-IAM
 * (table profil_cree) à côté du rôle composite du fournisseur d'identité : il rend le profil attribuable
 * et supprimable, et retient s'il est privilégié (permission IAM_* ou PLATFORM_*).
 */
public record ProfilCree(String code, String description, boolean privilegie, String creePar, Instant creeLe) {}
