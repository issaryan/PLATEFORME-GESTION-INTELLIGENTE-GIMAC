import { dictionnaire } from '@gimac/socle'

// Dictionnaires d'états (F0 §A5) : clé i18n « <préfixe>.<ÉTAT> » et tonalité du StatusBadge.
export const etatLiaison = dictionnaire('liaison.etat', {
  OUVERTE: 'neutral', EN_HOMOLOGATION: 'info', GELEE: 'crit', APTE_CERTIFICATION: 'info', MISE_EN_SERVICE_AUTORISEE: 'ok', CERTIFIEE: 'ok', CLOSE: 'neutral',
})
export const etatOperation = dictionnaire('operation.etat', {
  VERROUILLEE: 'neutral', ELIGIBLE: 'info', PREUVE_DEPOSEE: 'warn', VALIDEE_HORODATAGE_EN_ATTENTE: 'warn', CLOSE: 'ok', ROUVERTE: 'crit', HORS_PERIMETRE: 'neutral',
})
export const etatScope = dictionnaire('perimetre.etat', { A_ENGAGER: 'neutral', EN_COURS: 'info', COUVERT: 'ok', HORS_PERIMETRE: 'neutral' })
export const etatReferentiel = dictionnaire('referentiel.etat', { BROUILLON: 'info', PUBLIEE: 'ok', RETIREE: 'neutral' })
export const verdictCampagne = dictionnaire('verdict', { FAVORABLE: 'ok', DEFAVORABLE: 'crit', NON_EXECUTE: 'neutral' })
export const etatQuitus = dictionnaire('quitus.etat', { ATTENDU: 'info', SIGNE_FAVORABLE: 'ok', SIGNE_DEFAVORABLE: 'crit', CADUC: 'warn', ANNULE: 'neutral', NON_REQUIS: 'neutral' })
export const etatDossier = dictionnaire('dossier.etat', { OUVERT: 'info', AUTORISE: 'ok', MIS_EN_SERVICE: 'ok', COMPLET: 'ok', ATTESTE: 'ok' })
export const etatAttestation = dictionnaire('attestation.etat', { EN_VIGUEUR: 'ok', SUSPENDUE: 'warn', REVOQUEE: 'crit', REMPLACEE: 'neutral' })
export const etatScellement = dictionnaire('scellement.etat', { EN_ATTENTE: 'warn', SCELLE: 'ok', REJETE: 'crit' })
