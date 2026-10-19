import type { CSSProperties } from 'react'
import { cx } from '../../lib/cx'
import { useLibelle } from '../../i18n/LangueProvider'

// Grille d'agenda (demande du 19/10/2026, docs/lot1/mon-activite.md §2.1) : vues jour, semaine et mois.
// Jour et semaine : colonne des heures, ligne « toute la journée », événements placés à leur heure, chevauchements
// côte à côte, ligne de l'instant présent ; une case libre crée un événement à cette heure. Mois : six semaines,
// trois événements par jour au plus puis « + n autres ». Chaque catégorie prend une teinte de la palette de données
// (gm-data-1 à 8) portée par une pastille, jamais par un filet ; la couleur double toujours un libellé.

export interface AgendaEvent {
  id: string
  titre: string
  /** Instants ISO 8601 avec fuseau ; fin exclusive. */
  debut: string
  fin: string
  journeeEntiere?: boolean
  /** Libellé de la catégorie, affiché et lu. */
  categorie: string
  /** Teinte de la palette de données, de 1 à 8. */
  couleur: number
  lieu?: string | null
  /** Invitation sans réponse. */
  enAttente?: boolean
}

export interface AgendaGridProps {
  vue: 'jour' | 'semaine' | 'mois'
  /** Jours affichés (AAAA-MM-JJ) : 1 (jour), 7 (semaine) ou 42 (mois, lundi de la première semaine en tête). */
  jours: string[]
  /** Mois affiché (AAAA-MM) : les jours hors du mois sont atténués. */
  mois?: string
  evenements: AgendaEvent[]
  aujourdhui: string
  /** Instant présent (ISO) : ligne de l'heure courante dans la colonne d'aujourd'hui. */
  maintenant?: string
  /** Plage d'heures visible (0 à 24). */
  heureDebut?: number
  heureFin?: number
  label: string
  /** Libellé d'un jour (en-têtes, noms accessibles). */
  libelleJour: (jour: string) => string
  /** Heure locale « HH:MM » d'un instant. */
  heure: (iso: string) => string
  /** Jour local (AAAA-MM-JJ) et minutes depuis minuit d'un instant, dans le fuseau de l'application. */
  jourDe: (iso: string) => string
  minutesDe: (iso: string) => number
  onOuvrir: (id: string) => void
  /** Case libre : création à ce jour et, en jour ou semaine, à cette heure. Absent : lecture seule. */
  onCreer?: (jour: string, heure?: number) => void
  /** « + n autres » de la vue mois : ouvre la vue du jour. */
  onVoirJour?: (jour: string) => void
}

const HAUTEUR_HEURE = 48
const MAX_MOIS = 3

/** Un événement couvre-t-il ce jour (fin exclusive) ? */
function couvre(e: AgendaEvent, jour: string, jourDe: (iso: string) => string): boolean {
  const fin = new Date(new Date(e.fin).getTime() - 1).toISOString()
  return jourDe(e.debut) <= jour && jourDe(fin) >= jour
}

type Place = { e: AgendaEvent; debut: number; fin: number; colonne: number; colonnes: number }

/** Placement des événements horaires d'un jour : début et fin en minutes, colonnes des chevauchements. */
export function placer(evenements: AgendaEvent[], jour: string, jourDe: (iso: string) => string, minutesDe: (iso: string) => number): Place[] {
  const l = evenements
    .filter((e) => !e.journeeEntiere && couvre(e, jour, jourDe))
    .map((e) => ({
      e,
      debut: jourDe(e.debut) < jour ? 0 : minutesDe(e.debut),
      fin: jourDe(new Date(new Date(e.fin).getTime() - 1).toISOString()) > jour ? 24 * 60 : Math.max(minutesDe(e.fin), minutesDe(e.debut) + 15),
      colonne: 0,
      colonnes: 1,
    }))
    .sort((a, b) => a.debut - b.debut || b.fin - a.fin)
  // Groupes de chevauchement : dans chacun, chaque événement prend la première colonne libre.
  let groupe: Place[] = []
  let finGroupe = -1
  const clore = () => {
    const n = Math.max(1, ...groupe.map((p) => p.colonne + 1))
    groupe.forEach((p) => (p.colonnes = n))
    groupe = []
  }
  for (const p of l) {
    if (p.debut >= finGroupe) clore()
    const prises = new Set(groupe.filter((q) => q.fin > p.debut).map((q) => q.colonne))
    let c = 0
    while (prises.has(c)) c++
    p.colonne = c
    groupe.push(p)
    finGroupe = Math.max(finGroupe, p.fin)
  }
  clore()
  return l
}

function Pastille({ couleur }: { couleur: number }) {
  return <span className="gm-agenda-pastille" style={{ '--gm-agenda-couleur': `var(--gm-data-${Math.min(8, Math.max(1, couleur))})` } as CSSProperties} aria-hidden="true" />
}

function Puce({ e, horaire, onOuvrir }: { e: AgendaEvent; horaire: string; onOuvrir: () => void }) {
  const t = useLibelle()
  return (
    <button type="button" className={cx('gm-agenda-puce', e.enAttente && 'gm-agenda-attente')} onClick={onOuvrir}
      aria-label={[horaire, e.titre, e.categorie, e.enAttente ? t('agenda.enAttente') : null].filter(Boolean).join(' · ')} title={e.titre}>
      <Pastille couleur={e.couleur} />
      {horaire ? <span className="gm-agenda-heure">{horaire}</span> : null}
      <span className="gm-agenda-titre">{e.titre}</span>
    </button>
  )
}

export function AgendaGrid(p: AgendaGridProps) {
  const t = useLibelle()
  if (p.vue === 'mois') return <VueMois {...p} />
  const debut = p.heureDebut ?? 7
  const fin = p.heureFin ?? 20
  const heures = Array.from({ length: fin - debut }, (_, i) => debut + i)
  const journee = (j: string) => p.evenements.filter((e) => e.journeeEntiere && couvre(e, j, p.jourDe))
  const maintenant = p.maintenant ? p.minutesDe(p.maintenant) : null
  return (
    <div className="gm-agenda" role="group" aria-label={p.label} style={{ '--gm-agenda-jours': p.jours.length } as CSSProperties}>
      <div className="gm-agenda-entete">
        <span className="gm-agenda-coin" />
        {p.jours.map((j) => (
          <div key={j} className={cx('gm-agenda-jour', j === p.aujourdhui && 'gm-agenda-aujourdhui')}>{p.libelleJour(j)}</div>
        ))}
      </div>
      <div className="gm-agenda-entete gm-agenda-journee">
        <span className="gm-agenda-coin gm-field-help">{t('agenda.toutLeJour')}</span>
        {p.jours.map((j) => (
          <div key={j} className="gm-agenda-journee-case">
            {journee(j).map((e) => <Puce key={e.id} e={e} horaire="" onOuvrir={() => p.onOuvrir(e.id)} />)}
          </div>
        ))}
      </div>
      <div className="gm-agenda-corps">
        <div className="gm-agenda-heures" aria-hidden="true">
          {heures.map((h) => <span key={h} style={{ height: HAUTEUR_HEURE }}>{`${String(h).padStart(2, '0')}:00`}</span>)}
        </div>
        {p.jours.map((j) => (
          <div key={j} className={cx('gm-agenda-colonne', j === p.aujourdhui && 'gm-agenda-aujourdhui')} style={{ height: heures.length * HAUTEUR_HEURE }}>
            {heures.map((h) =>
              p.onCreer ? (
                <button key={h} type="button" className="gm-agenda-case" style={{ height: HAUTEUR_HEURE }} onClick={() => p.onCreer?.(j, h)}
                  aria-label={t('agenda.creerA', { jour: p.libelleJour(j), heure: `${String(h).padStart(2, '0')}:00` })} />
              ) : <span key={h} className="gm-agenda-case" style={{ height: HAUTEUR_HEURE }} />,
            )}
            {placer(p.evenements, j, p.jourDe, p.minutesDe).map((x) => {
              const haut = Math.max(0, (x.debut - debut * 60) / 60) * HAUTEUR_HEURE
              const bas = Math.min(heures.length, (x.fin - debut * 60) / 60) * HAUTEUR_HEURE
              if (bas <= 0 || haut >= heures.length * HAUTEUR_HEURE) return null
              return (
                <div key={x.e.id} className="gm-agenda-evenement" style={{ top: haut, height: Math.max(22, bas - haut - 2), left: `calc(${(100 * x.colonne) / x.colonnes}% + 2px)`, width: `calc(${100 / x.colonnes}% - 4px)` }}>
                  <Puce e={x.e} horaire={t('agenda.deA', { debut: p.heure(x.e.debut), fin: p.heure(x.e.fin) })} onOuvrir={() => p.onOuvrir(x.e.id)} />
                  {x.e.lieu && bas - haut > 50 ? <span className="gm-agenda-lieu">{x.e.lieu}</span> : null}
                </div>
              )
            })}
            {j === p.aujourdhui && maintenant !== null && maintenant >= debut * 60 && maintenant < fin * 60 ? (
              <span className="gm-agenda-maintenant" style={{ top: ((maintenant - debut * 60) / 60) * HAUTEUR_HEURE }} role="img" aria-label={t('agenda.maintenant')} />
            ) : null}
          </div>
        ))}
      </div>
    </div>
  )
}

function VueMois(p: AgendaGridProps) {
  const t = useLibelle()
  const semaine = p.jours.slice(0, 7)
  return (
    <div className="gm-agenda-mois" role="group" aria-label={p.label}>
      {semaine.map((j) => <div key={j} className="gm-agenda-jour gm-agenda-mois-entete">{p.libelleJour(j).split(' ')[0]}</div>)}
      {p.jours.map((j) => {
        const du = p.evenements.filter((e) => couvre(e, j, p.jourDe)).sort((a, b) => (a.journeeEntiere === b.journeeEntiere ? a.debut.localeCompare(b.debut) : a.journeeEntiere ? -1 : 1))
        const hors = p.mois && !j.startsWith(p.mois)
        return (
          <div key={j} className={cx('gm-agenda-mois-case', hors && 'gm-agenda-hors', j === p.aujourdhui && 'gm-agenda-aujourdhui')} aria-label={p.libelleJour(j)} role="group">
            {p.onCreer ? (
              <button type="button" className="gm-agenda-numero" onClick={() => p.onCreer?.(j)} aria-label={t('agenda.creerLe', { jour: p.libelleJour(j) })}>{Number(j.slice(8))}</button>
            ) : <span className="gm-agenda-numero">{Number(j.slice(8))}</span>}
            {du.slice(0, MAX_MOIS).map((e) => <Puce key={e.id} e={e} horaire={e.journeeEntiere ? '' : p.heure(e.debut)} onOuvrir={() => p.onOuvrir(e.id)} />)}
            {du.length > MAX_MOIS ? (
              <button type="button" className="gm-agenda-autres" onClick={() => p.onVoirJour?.(j)}>{t('agenda.autres', { n: du.length - MAX_MOIS })}</button>
            ) : null}
          </div>
        )
      })}
    </div>
  )
}
