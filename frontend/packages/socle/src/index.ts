// Socle d'interface du lot 1 (PI-GIMAC-01 F0).
export { P, SESSION, CONNEXION, DECONNEXION, API_CONNEXION, APPAREILS, HISTORIQUE_CONNEXIONS, prefixeIam, type Couche } from './http/prefixes'
export { urlAvatar, avatarsSimules, photoDemo } from './avatar/avatar'
export { ApiError, estApiError, type Problem } from './http/errors'
export { http, configureHttp, reprDigestToHex, type RequestOptions, type HttpHooks, type CsrfConfig } from './http/client'
export {
  televerser, depotsHttp, type ServiceDepots, type Depot, type EtatDepot, type OuvertureDepot, type Progression, type Televersement, type OptionsTeleversement,
} from './http/televersement'
export { openLiveChannel, type LiveTopic, type LiveEvent, type EtatCanal } from './live/live'
export { SIGNAL, DUREE_SAISIE_MS, initialiserPresence, appliquerSignal, viderSignaux, usePresence, useEnLigne, useSaisies, type EtatPresence } from './live/signaux'
export { getReauthAssertion, ReauthCancelled, CleSecuriteIndisponible, cleSecuriteDisponible, refusReauth, estAnnulation, b64u, fromB64u, type SignPurpose, type RefusReauth } from './reauth/webauthn'
export { sha256Hex } from './crypto/sha256'
export { enrolerCleSecurite, optionsCreation, type OptionsEnrolement, type CleSecurite } from './reauth/enrolement'
export * from './ressource/types'
export { QueryBoundary, EtatsRequete, type QueryBoundaryProps } from './requete/QueryBoundary'
export { initI18n, LANGUES, type Langue, type Catalogue } from './i18n/i18n'
export { Enveloppe, type EnveloppeProps, type EntreeNavigation, type GroupeNavigation } from './enveloppe/Enveloppe'
export { Preferences } from './enveloppe/Preferences'
export { RechercheGlobale, type ResultatRecherche, type SourceRecherche, type EcranRecherche } from './enveloppe/RechercheGlobale'
export { usePreferences, langueInitiale, lirePreference, CLE_THEME, CLE_LANGUE } from './enveloppe/usePreferences'
export { PageConnexion, type PageConnexionProps } from './connexion/PageConnexion'
export { CODES_CONNEXION, type ServiceConnexion, type DemandeConnexion, type ReponseConnexion, type EtapePremierAcces } from './connexion/modele'
export { connexionHttp } from './connexion/http'
export { connexionSimulee, simulationConnexion, MOT_DE_PASSE_DEMO, ETAPES_PREMIER_ACCES, type CompteConnexionDemo, type OptionsConnexionSimulee } from './connexion/simule'
export { ConnexionProvider, useConnexion } from './connexion/contexte'
export { violationsMotDePasse, violationsDe, LONGUEUR_MIN_MOT_DE_PASSE, type Violation } from './connexion/politique'
export { RappelPolitique, ListeViolations, useLibelleViolation } from './connexion/MotDePasse'
export { SessionProvider, SessionActive, GardeSession, useSession, usePermissions, aPermission, chargerSession, type Session } from './session/session'
export {
  deconnecter, reconnecter, surNonAutorise, adresseConnexion, retourSur, retourCourant, navigation as navigationDocument, type MotifConnexion,
} from './session/deconnexion'
export { lireSessionDemo, ouvrirSessionDemo, fermerSessionDemo, CLE_SESSION_DEMO, type SessionDemo } from './session/demo'
export {
  SurveillanceInactivite, dureeInactivite, prolongerSession, signalerActivite, minutesSecondes, INACTIVITE_MAX_MS, PREAVIS_MS, CANAL_SESSION, type SurveillanceInactiviteProps,
} from './session/inactivite'
export { PageDeconnexion } from './session/PageDeconnexion'
export { LangueComposants } from './i18n/LangueComposants'
