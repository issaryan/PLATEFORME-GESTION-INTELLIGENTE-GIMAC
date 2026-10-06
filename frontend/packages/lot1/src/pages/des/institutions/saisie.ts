import type { Candidature, CategorieInstitution } from '../../../modele/participant'
import { aujourdhui, depuisSaisie } from '../../../modele/jours'
import { categorieOptions, paysOptions } from '../../../ressources/institution'

// Saisie de la prise de contact et de la fiche (DES-07) : normalisation et contrôles alignés sur
// InstitutionRequests.CandidatureRequest de MS-PARTICIPANT, pour signaler un champ avant l'envoi.
// Le service reste juge : ses refus par champ (fieldErrors) s'affichent aussi sous le champ.

/** Champs du formulaire, noms du corps de la requête. */
export type ChampCandidature = keyof Candidature

/** Erreur d'un champ : clé i18n et valeurs d'interpolation. */
export type ErreurChamp = { cle: string; valeurs?: Record<string, unknown> }

export const BIC = /^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$/
export const CODE_BEAC = /^[A-Z0-9]+$/
export const LONGUEURS: Partial<Record<ChampCandidature, number>> = { nom: 255, sigle: 50, codeBanqueBeac: 10, originePriseDeContact: 255, projetEnvisage: 1000 }

const ISO = /^(\d{4})-(\d{2})-(\d{2})$/

const texte = (v: unknown) => (typeof v === 'string' && v.trim() !== '' ? v.trim() : null)

/** Code BIC ou code banque BEAC : majuscules, espaces retirés (saisie recopiée d'un courrier). */
export const normaliserCode = (v: unknown): string | null => {
  const s = typeof v === 'string' ? v.replace(/\s+/g, '').toUpperCase() : ''
  return s === '' ? null : s
}

/**
 * Date de prise de contact en AAAA-MM-JJ : la saisie du champ (JJ/MM/AAAA) est convertie, une date déjà
 * ISO (valeur initiale d'une fiche) passe ; undefined si la saisie n'est pas une date existante.
 */
export function dateContact(v: unknown): string | null | undefined {
  const s = texte(v)
  if (s === null) return null
  if (ISO.test(s)) return depuisSaisie(`${s.slice(8, 10)}/${s.slice(5, 7)}/${s.slice(0, 4)}`) ?? undefined
  return depuisSaisie(s) ?? undefined
}

/** Corps de la requête (POST ou PUT /bff/des/participant/institutions). */
export const candidature = (v: Record<string, unknown>): Candidature => ({
  nom: texte(v.nom) ?? '',
  sigle: texte(v.sigle),
  pays: texte(v.pays) ?? '',
  categorie: (texte(v.categorie) ?? '') as CategorieInstitution,
  codeIdentification: normaliserCode(v.codeIdentification),
  codeBanqueBeac: normaliserCode(v.codeBanqueBeac),
  datePriseDeContact: dateContact(v.datePriseDeContact) ?? null,
  originePriseDeContact: texte(v.originePriseDeContact),
  projetEnvisage: texte(v.projetEnvisage),
})

const PAYS = new Set(paysOptions.map((o) => o.value))
const CATEGORIES = new Set(categorieOptions.map((o) => o.value))

/**
 * Contrôles du service, appliqués avant l'envoi : un message par champ refusé. La date ne peut être
 * postérieure au jour de Douala (fuseau des agents, comme le service).
 */
export function verifierCandidature(v: Record<string, unknown>, jour: string = aujourdhui()): Partial<Record<ChampCandidature, ErreurChamp>> {
  const c = candidature(v)
  const e: Partial<Record<ChampCandidature, ErreurChamp>> = {}
  if (!c.nom) e.nom = { cle: 'institution.erreur.requis' }
  if (!c.pays) e.pays = { cle: 'institution.erreur.requis' }
  else if (!PAYS.has(c.pays)) e.pays = { cle: 'institution.erreur.choix' }
  if (!c.categorie) e.categorie = { cle: 'institution.erreur.requis' }
  else if (!CATEGORIES.has(c.categorie)) e.categorie = { cle: 'institution.erreur.choix' }
  if (c.codeIdentification && !BIC.test(c.codeIdentification)) e.codeIdentification = { cle: 'institution.erreur.codeIdentification' }
  if (c.codeBanqueBeac && (!CODE_BEAC.test(c.codeBanqueBeac) || c.codeBanqueBeac.length > 10)) e.codeBanqueBeac = { cle: 'institution.erreur.codeBanqueBeac' }
  const date = dateContact(v.datePriseDeContact)
  if (date === undefined) e.datePriseDeContact = { cle: 'institution.erreur.dateFormat' }
  else if (date !== null && date > jour) e.datePriseDeContact = { cle: 'institution.erreur.dateFuture' }
  for (const [champ, max] of Object.entries(LONGUEURS) as [ChampCandidature, number][]) {
    const valeur = c[champ]
    if (!e[champ] && typeof valeur === 'string' && valeur.length > max) e[champ] = { cle: 'institution.erreur.longueur', valeurs: { max } }
  }
  return e
}

/** Refus du service par champ (details.champs) : libellé clair du champ, générique pour un champ inconnu. */
export function erreursDuService(champs: Record<string, string>, t: (cle: string, o?: Record<string, unknown>) => string): Record<string, string> {
  return Object.fromEntries(
    Object.keys(champs).map((champ) => [champ, t(`institution.erreur.service.${champ}`, { defaultValue: t('institution.erreur.service') })]),
  )
}
