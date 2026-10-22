package cm.gimac.des.homologation.domain.sandbox;

import cm.gimac.des.homologation.domain.enumeration.EtatLiaison;

/**
 * État d'une liaison vu par la Sandbox GIMAC (US-EXI-16, CC-SBX-GIMAC-02 §6.1) : une campagne ne s'ouvre que sur une
 * liaison OUVERTE ; GELEE et CLOSE l'interdisent. Les états techniques actifs de la liaison (ouverte, en homologation,
 * apte, mise en service autorisée, certifiée) sont tous OUVERTE pour la sandbox.
 */
public enum StatutSandbox {
    OUVERTE,
    GELEE,
    CLOSE;

    public static StatutSandbox de(EtatLiaison etat) {
        if (etat == EtatLiaison.GELEE) {
            return GELEE;
        }
        return etat == EtatLiaison.CLOSE ? CLOSE : OUVERTE;
    }
}
