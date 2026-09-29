package cm.gimac.des.gateway.domain.supervision;

/** Conteneur vu par cAdvisor : CPU en cœurs consommés, mémoire de travail en octets. */
public record EtatConteneur(String nom, Double cpuCoeurs, Double memoireOctets) {}
