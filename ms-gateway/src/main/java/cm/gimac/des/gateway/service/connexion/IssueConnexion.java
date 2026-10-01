package cm.gimac.des.gateway.service.connexion;

import cm.gimac.des.gateway.domain.connexion.ResultatConnexion;
import org.springframework.security.core.Authentication;

/** Issue d'une connexion réussie et authentification de la session ouverte (nulle si aucune session). */
public record IssueConnexion(ResultatConnexion resultat, Authentication authentification) {}
