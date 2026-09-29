package cm.gimac.des.gateway.domain.supervision;

import java.time.Instant;

/** Alerte active d'Alertmanager : nom, sévérité, résumé, depuis, cible (instance) et job. */
public record Alerte(String nom, String severite, String resume, String description, Instant depuis, String cible, String job, String etat) {}
