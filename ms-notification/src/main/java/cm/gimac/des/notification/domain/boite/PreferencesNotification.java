package cm.gimac.des.notification.domain.boite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * Préférences de notification d'un compte (docs/lot1/notifications.md §3) : notification du système,
 * son, et types actifs. Défauts : système actif, son inactif, tous les types actifs. Un type désactivé
 * ne crée plus de notification.
 */
@Entity
@Table(name = "preference_notification")
public class PreferencesNotification {

    @Id
    @Column(name = "compte_id", length = 80)
    private String compteId;

    @Column(name = "systeme", nullable = false)
    private boolean systeme = true;

    @Column(name = "son", nullable = false)
    private boolean son = false;

    @Column(name = "type_message", nullable = false)
    private boolean message = true;

    @Column(name = "type_tache", nullable = false)
    private boolean tache = true;

    @Column(name = "type_rapport", nullable = false)
    private boolean rapport = true;

    @Column(name = "type_demande", nullable = false)
    private boolean demande = true;

    @Column(name = "type_mention", nullable = false)
    private boolean mention = true;

    @Column(name = "type_reponse", nullable = false)
    private boolean reponse = true;

    @Column(name = "type_processus", nullable = false)
    private boolean processus = true;

    @Column(name = "type_homologation", nullable = false)
    private boolean homologation = true;

    @Column(name = "modifie_le", nullable = false)
    private Instant modifieLe;

    protected PreferencesNotification() {}

    /** Préférences par défaut du compte (non enregistrées tant qu'il ne les modifie pas). */
    public static PreferencesNotification defauts(String compteId, Instant le) {
        PreferencesNotification p = new PreferencesNotification();
        p.compteId = compteId;
        p.modifieLe = le;
        return p;
    }

    /** Nouvelles valeurs : un champ absent (null) garde sa valeur ; un type absent de la table aussi. */
    public void modifier(Boolean systeme, Boolean son, Map<TypeNotification, Boolean> types, Instant le) {
        if (systeme != null) {
            this.systeme = systeme;
        }
        if (son != null) {
            this.son = son;
        }
        if (types != null) {
            types.forEach((t, actif) -> {
                if (actif != null) {
                    activer(t, actif);
                }
            });
        }
        this.modifieLe = le;
    }

    public boolean accepte(TypeNotification type) {
        return switch (type) {
            case MESSAGE -> message;
            case TACHE -> tache;
            case RAPPORT -> rapport;
            case DEMANDE -> demande;
            case MENTION -> mention;
            case REPONSE -> reponse;
            case PROCESSUS -> processus;
            case HOMOLOGATION -> homologation;
        };
    }

    private void activer(TypeNotification type, boolean actif) {
        switch (type) {
            case MESSAGE -> message = actif;
            case TACHE -> tache = actif;
            case RAPPORT -> rapport = actif;
            case DEMANDE -> demande = actif;
            case MENTION -> mention = actif;
            case REPONSE -> reponse = actif;
            case PROCESSUS -> processus = actif;
            case HOMOLOGATION -> homologation = actif;
        }
    }

    /** Types et leur activation, dans l'ordre de l'énumération. */
    public Map<TypeNotification, Boolean> types() {
        Map<TypeNotification, Boolean> m = new EnumMap<>(TypeNotification.class);
        for (TypeNotification t : TypeNotification.values()) {
            m.put(t, accepte(t));
        }
        return m;
    }

    public String getCompteId() {
        return compteId;
    }

    public boolean isSysteme() {
        return systeme;
    }

    public boolean isSon() {
        return son;
    }

    public Instant getModifieLe() {
        return modifieLe;
    }
}
