package cm.gimac.des.pilotage.domain.processus;

/** Version d'un processus (§1, comme D-77) : BROUILLON (modifiable), PUBLIEE (lançable), RETIREE (historique). */
public enum EtatProcessus {
    BROUILLON,
    PUBLIEE,
    RETIREE,
}
