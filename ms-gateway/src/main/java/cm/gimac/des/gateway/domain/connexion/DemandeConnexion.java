package cm.gimac.des.gateway.domain.connexion;

import cm.gimac.des.gateway.domain.live.Couche;

/**
 * Tentative de connexion : identifiant, mot de passe, code TOTP éventuel, couche visée et adresse
 * IP d'origine. Le mot de passe et le code ne sont jamais journalisés.
 */
public record DemandeConnexion(String identifiant, String motDePasse, String code, Couche couche, String adresseIp) {
    public boolean avecCode() {
        return code != null && !code.isBlank();
    }

    @Override
    public String toString() {
        return "DemandeConnexion[identifiant=" + MasqueIdentifiant.masquer(identifiant) + ", couche=" + couche + ", ip=" + adresseIp + "]";
    }
}
