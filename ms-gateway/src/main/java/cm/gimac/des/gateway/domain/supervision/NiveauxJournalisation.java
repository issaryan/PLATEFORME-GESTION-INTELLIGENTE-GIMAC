package cm.gimac.des.gateway.domain.supervision;

import java.util.List;

/** Niveaux de journalisation d'un service (/management/loggers). */
public record NiveauxJournalisation(String service, List<String> niveauxDisponibles, List<NiveauLogger> loggers) {
    public NiveauxJournalisation {
        niveauxDisponibles = List.copyOf(niveauxDisponibles);
        loggers = List.copyOf(loggers);
    }
}
