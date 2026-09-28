package cm.gimac.des.incident.domain.enumeration;

/**
 * Automate des tickets (REC D-08) : OUVERT → QUALIFIE → EN_COURS ⇄
 * EN_ATTENTE_TIERS → RESOLU → CLOS, avec REOUVERT. Pas de CLOS sans RESOLU,
 * hors fermeture d'un doublon depuis OUVERT (RG-36, algorithme 6.39).
 */
public enum TicketState {
    OUVERT,
    QUALIFIE,
    EN_COURS,
    EN_ATTENTE_TIERS,
    RESOLU,
    CLOS,
    REOUVERT,
}
