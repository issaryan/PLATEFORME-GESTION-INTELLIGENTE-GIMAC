import { Fragment, useEffect, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, StatusBadge, Tag } from '@gimac/design-system'
import { estApiError } from '@gimac/socle'
import type { CategorieEvenement, EtatRapportDuJour, EtatTache, Priorite, StatutActivite, TypeRapport, Unite } from '../../../modele/pilotage'
import { ajouterJours, aujourdhui, heureDe, jourDe, lundiDeSemaine } from '../../../modele/jours'
import { useUnites } from '../../../hooks/pilotage'

// Compositions communes de « Mon activité » (D-76) : dates du fuseau de Douala, pastilles d'état, refus
// traduits par leur code, rendu d'un texte en Markdown simple, navigation d'un jour ou d'une semaine.

type Statut = 'ok' | 'warn' | 'crit' | 'info' | 'neutral'

const STATUT_ACTIVITE: Record<StatutActivite, Statut> = { A_FAIRE: 'neutral', EN_COURS: 'info', FAITE: 'ok', REPORTEE: 'warn', ANNULEE: 'neutral' }
export const ETAT_TACHE: Record<EtatTache, Statut> = { A_PRENDRE: 'warn', OUVERTE: 'neutral', EN_COURS: 'info', BLOQUEE: 'crit', TERMINEE: 'ok', ANNULEE: 'neutral' }
const PRIORITE: Record<Priorite, Statut> = { BASSE: 'neutral', NORMALE: 'info', HAUTE: 'warn', URGENTE: 'crit' }
const ETAT_RAPPORT: Record<EtatRapportDuJour, Statut> = { ABSENT: 'warn', BROUILLON: 'info', SOUMIS: 'ok', VISE: 'ok', DEPOSE: 'neutral' }

export function BadgeActivite({ statut }: { statut: StatutActivite }) {
  const { t } = useTranslation()
  return <StatusBadge status={STATUT_ACTIVITE[statut]} label={t(`act.statut.${statut}`)} />
}
export function BadgeTache({ etat, enRetard }: { etat: EtatTache; enRetard?: boolean }) {
  const { t } = useTranslation()
  return (
    <span className="gm-row" style={{ gap: 'var(--gm-space-1)' }}>
      <StatusBadge status={ETAT_TACHE[etat]} label={t(`act.tache.etat.${etat}`)} />
      {enRetard ? <StatusBadge status="crit" icon="clock" label={t('act.tache.enRetard')} /> : null}
    </span>
  )
}
export function BadgePriorite({ priorite }: { priorite: Priorite }) {
  const { t } = useTranslation()
  return <StatusBadge status={PRIORITE[priorite]} icon={null} label={t(`act.priorite.${priorite}`)} />
}
export function BadgeRapport({ etat }: { etat: EtatRapportDuJour }) {
  const { t } = useTranslation()
  return <StatusBadge status={ETAT_RAPPORT[etat]} label={t(`act.rapport.etat.${etat}`)} />
}
export function TagCategorie({ categorie }: { categorie: CategorieEvenement }) {
  const { t } = useTranslation()
  return <Tag>{t(`act.categorie.${categorie}`)}</Tag>
}

/** Formats de date de la langue courante, au fuseau de Douala. */
export function useDates() {
  const { t, i18n } = useTranslation()
  const format = (jour: string, o: Intl.DateTimeFormatOptions) => new Intl.DateTimeFormat(i18n.language, { ...o, timeZone: 'UTC' }).format(new Date(`${jour}T12:00:00Z`))
  return {
    /** « lundi 5 octobre 2026 ». */
    jourLong: (jour: string) => format(jour, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }),
    /** « lun. 5 oct. ». */
    jourCourt: (jour: string) => format(jour, { weekday: 'short', day: 'numeric', month: 'short' }),
    /** « 05/10/2026 ». */
    jour: (jour: string) => format(jour, { day: '2-digit', month: '2-digit', year: 'numeric' }),
    /** Jour et heure d'un instant (« 05/10/2026 14:30 »). */
    instant: (iso: string) => `${format(jourDe(iso), { day: '2-digit', month: '2-digit', year: 'numeric' })} ${heureDe(iso)}`,
    heure: heureDe,
    /** Période d'un rapport : le jour, ou la semaine du lundi au dimanche. */
    periode: (type: TypeRapport, periode: string) =>
      type === 'QUOTIDIEN' ? format(periode, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
        : t('act.rapport.semaineDu', { semaine: periode.slice(6), du: format(lundiDeSemaine(periode), { day: 'numeric', month: 'short' }), au: format(ajouterJours(lundiDeSemaine(periode), 6), { day: 'numeric', month: 'short', year: 'numeric' }) }),
    /** Durée en heures et minutes (« 1 h 30 »). */
    duree: (minutes: number | null) => (minutes === null || minutes === undefined ? '' : minutes < 60 ? `${minutes} min` : `${Math.floor(minutes / 60)} h${minutes % 60 ? ` ${String(minutes % 60).padStart(2, '0')}` : ''}`),
  }
}

/** Refus du service traduit par son code (act.erreur.<code>), message du service à défaut ; identifiant de corrélation conservé. */
export function useErreur() {
  const { t } = useTranslation()
  return (e: unknown): { titre: string; correlationId?: string } | undefined => {
    if (!e) return undefined
    if (estApiError(e)) return { titre: t(`act.erreur.${e.code}`, { defaultValue: e.message || t('act.erreur.generique') }), correlationId: e.correlationId || undefined }
    return { titre: t('act.erreur.generique') }
  }
}
/** Forme attendue par CrudDialog. */
export const pourDialogue = (e: { titre: string; correlationId?: string } | undefined) => (e ? { title: e.titre, correlationId: e.correlationId } : undefined)

/** Libellé d'une unité par son code (catalogue de MS-IAM), le code à défaut. */
export function useLibelleUnite() {
  const unites = useUnites()
  return (code: string | null | undefined) => (code ? (unites.data ?? []).find((u: Unite) => u.code === code)?.libelle ?? code : '')
}

/** Largeur d'écran étroite (téléphone) : l'agenda passe de la semaine au jour. */
export function useEcranEtroit(seuil = 720) {
  const [etroit, setEtroit] = useState(() => window.innerWidth < seuil)
  useEffect(() => {
    const redim = () => setEtroit(window.innerWidth < seuil)
    window.addEventListener('resize', redim)
    return () => window.removeEventListener('resize', redim)
  }, [seuil])
  return etroit
}

/** Navigation d'une période : précédente, revenir à aujourd'hui, suivante. */
export function NavigationPeriode({ libelle, onPrecedent, onAujourdhui, onSuivant, precedent, suivant, aujourdhuiLibelle }: {
  libelle: string; onPrecedent: () => void; onAujourdhui?: () => void; onSuivant: () => void; precedent: string; suivant: string; aujourdhuiLibelle: string
}) {
  return (
    <div className="gm-row" role="group" aria-label={libelle} style={{ gap: 'var(--gm-space-2)', flexWrap: 'wrap' }}>
      <Button size="sm" variant="ghost" icon="arrow-left" onClick={onPrecedent}>{precedent}</Button>
      <strong aria-live="polite">{libelle}</strong>
      <Button size="sm" variant="ghost" onClick={onSuivant}>{suivant}</Button>
      {onAujourdhui ? <Button size="sm" variant="secondary" icon="calendar" onClick={onAujourdhui}>{aujourdhuiLibelle}</Button> : null}
    </div>
  )
}

/** Jour demandé dans l'adresse (?jour=AAAA-MM-JJ), aujourd'hui à défaut. */
export const jourDemande = (valeur: string | null) => (valeur && /^\d{4}-\d{2}-\d{2}$/.test(valeur) ? valeur : aujourdhui())

/** Gras en ligne (**texte**) d'une ligne de Markdown simple. */
function enLigne(texte: string): ReactNode[] {
  return texte.split(/(\*\*[^*]+\*\*)/g).filter(Boolean).map((m, i) => (m.startsWith('**') && m.endsWith('**') ? <strong key={i}>{m.slice(2, -2)}</strong> : <Fragment key={i}>{m}</Fragment>))
}

/**
 * Markdown simple d'un rapport (titres #, ## et ###, listes « - », gras, paragraphes), rendu en éléments :
 * le texte n'est jamais interprété comme du HTML.
 */
export function TexteRapport({ contenu }: { contenu: string }) {
  const blocs: ReactNode[] = []
  let liste: string[] = []
  const vider = () => {
    if (liste.length) blocs.push(<ul key={`l${blocs.length}`}>{liste.map((l, i) => <li key={i}>{enLigne(l)}</li>)}</ul>)
    liste = []
  }
  contenu.split('\n').forEach((brut) => {
    const ligne = brut.trimEnd()
    const titre = /^(#{1,3})\s+(.*)$/.exec(ligne)
    if (/^\s*[-*]\s+/.test(ligne)) {
      liste.push(ligne.replace(/^\s*[-*]\s+/, ''))
      return
    }
    vider()
    if (titre) {
      const niveau = titre[1].length
      blocs.push(niveau === 1 ? <h3 key={blocs.length} className="gm-type-h3" style={{ margin: 0 }}>{enLigne(titre[2])}</h3> : <h4 key={blocs.length} className="gm-type-body-strong" style={{ margin: 'var(--gm-space-2) 0 0' }}>{enLigne(titre[2])}</h4>)
    } else if (ligne.trim()) blocs.push(<p key={blocs.length} style={{ margin: 0 }}>{enLigne(ligne)}</p>)
  })
  vider()
  return <div className="gm-col gm-texte-rapport" style={{ gap: 'var(--gm-space-2)' }}>{blocs}</div>
}
