package cm.gimac.des.certification.application.port.out;

import cm.gimac.des.certification.domain.enumeration.EtatFicheChangement;

/**
 * Lecture de la fiche de changement du dispositif existant de gestion des changements : lue, jamais refaite
 * (D-56, D-16, RG-34). La plateforme n'y écrit rien.
 */
public interface FicheChangementPort {
    EtatFicheChangement lire(String reference);
}
