package cm.gimac.des.homologation.domain.parcours;

/** Statut du CR porté par la liaison comme étape courante (D-57). */
public enum StatutParcours {
    CADREE,
    EN_INTEGRATION,
    EN_TEST,
    CERTIFIEE,
    APPROUVEE_SECURITE,
    EN_PRODUCTION,
    SUSPENDUE,
    CLOSE_SANS_SUITE,
}
