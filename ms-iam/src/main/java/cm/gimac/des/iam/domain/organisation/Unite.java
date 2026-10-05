package cm.gimac.des.iam.domain.organisation;

/**
 * Unité organisationnelle du GIMAC (D-76, docs/lot1/mon-activite.md §1.1) : code stable et unique,
 * libellé, type, unité parente (nulle pour une direction) et état. Une unité inactive n'est plus
 * proposée au rattachement ; les rattachements existants restent lisibles.
 */
public record Unite(String code, String libelle, TypeUnite type, String parent, boolean active) {}
