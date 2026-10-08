package cm.gimac.des.homologation.domain.parcours;

/** État d'une opération instanciée du référentiel. */
public enum EtatOperationParcours {
    VERROUILLEE,
    A_FAIRE,
    EN_COURS,
    EN_VALIDATION,
    /** Travail accompli et validé, en attente des signatures exigées (D-91), dans leur ordre. */
    EN_SIGNATURE,
    FAITE,
    BLOQUEE,
    SANS_OBJET;

    /** Opération ouverte : son geste peut être accompli. */
    public boolean ouverte() {
        return this == A_FAIRE || this == EN_COURS;
    }

    /** Opération qui figure parmi les tâches d'un acteur. */
    public boolean enCours() {
        return this == A_FAIRE || this == EN_COURS || this == BLOQUEE || this == EN_VALIDATION || this == EN_SIGNATURE;
    }
}
