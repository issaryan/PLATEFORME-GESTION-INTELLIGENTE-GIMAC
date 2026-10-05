package cm.gimac.des.pilotage.application.port.in;

import java.util.Set;

/** Compte interne appelant : sujet du jeton, nom affiché et permissions qu'il porte. */
public record Appelant(String id, String nomAffiche, Set<String> permissions) {
    public static final String ACTIVITE = "PIL_ACTIVITY_OWN";
    public static final String EQUIPE = "PIL_TEAM_READ";
    public static final String CONFIER = "PIL_TASK_ASSIGN";

    public Appelant {
        permissions = Set.copyOf(permissions);
    }

    public boolean litEquipe() {
        return permissions.contains(EQUIPE);
    }

    public boolean confie() {
        return permissions.contains(CONFIER);
    }
}
