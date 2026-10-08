package cm.gimac.des.homologation.domain.parcours;

/**
 * Institution liée déclarée sur une liaison pour une fonction externe (D-93 §1) : banque partenaire,
 * personnalisateur ou toute fonction externe du catalogue. Elle tient la fonction (comptes de l'institution) ;
 * sans institution liée, l'opération reste à la main du conducteur.
 */
public record InstitutionLiee(String id, String nom) {}
