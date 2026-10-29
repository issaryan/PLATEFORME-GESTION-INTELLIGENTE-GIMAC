import { useEffect, useMemo, useRef, useState, type KeyboardEvent, type PointerEvent, type ReactNode } from 'react'
import type { Status } from '../../lib/types'
import { cx } from '../../lib/cx'
import {
  cheminAire,
  cheminCourbe,
  classeSerie,
  echelle,
  formatCompact,
  graduationsTemps,
  graduationsPoints,
  libelleGraduation,
  libellePeriode,
  type Granularite,
  type PointMesure,
} from '../../lib/graphiques'
import { IconButton } from '../../actions/IconButton/IconButton'
import { StatusBadge } from '../../affichage/StatusBadge/StatusBadge'
import { Tabs } from '../../navigation/Tabs/Tabs'
import { useLangue, useLibelle } from '../../i18n/LangueProvider'

export interface SerieMesure {
  nom: string
  points: PointMesure[]
}

export interface SerieTemporelleProps {
  title: string
  subtitle?: string
  /** Quatre séries d'identité au plus (ordre fixe) ; les suivantes sont tracées en gris, regroupées en « Autres ». */
  series: SerieMesure[]
  unit?: string
  /** Mise en forme d'une valeur (infobulle, axe, tableau) ; nombre compact par défaut. */
  format?: (v: number) => string
  /** Domaine de l'axe : une disponibilité part de 90 %, un pourcentage de charge de 0 à 100. */
  min?: number
  max?: number
  seuil?: number
  /** Lavis d'aire sous la courbe ; par défaut pour une série unique. */
  aire?: boolean
  hauteur?: number
  status?: Status
  statusLabel?: string
  /** Premier chargement : cadre et message d'attente. */
  chargement?: boolean
  /** Rechargement : le graphique précédent reste affiché, atténué (pas de flash ni de squelette). */
  rechargement?: boolean
  /** Message quand aucune mesure n'est disponible sur la période. */
  vide?: string
  /** Export de la vue (CSV, image) ; le bouton n'apparaît que s'il est fourni. */
  onExporter?: () => void
  /** Actions d'en-tête complémentaires (lien vers le détail…). */
  actions?: ReactNode
  /** Pas des points : instants (défaut), jours ou mois (comptes des tableaux de bord) ; règle l'axe et les libellés. */
  granularite?: Granularite
}

const MAX_IDENTITE = 4

// Courbe temporelle des métriques (supervision, tableaux de bord) : une seule échelle, axe du temps,
// trous pour les mesures absentes, réticule et infobulle listant toutes les séries (souris et clavier),
// légende dès deux séries, vue Tableau équivalente. Le texte reste en encre ; la couleur ne porte que
// l'identité de la série, jamais un statut.
export function SerieTemporelle(p: SerieTemporelleProps) {
  const t = useLibelle()
  const langue = useLangue()
  const [vue, setVue] = useState('graphique')
  const [largeur, setLargeur] = useState(640)
  const [curseur, setCurseur] = useState<number | null>(null)
  const cadre = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = cadre.current
    if (!el || typeof ResizeObserver === 'undefined') return
    const ro = new ResizeObserver((entrees) => {
      const w = Math.round(entrees[0].contentRect.width)
      if (w > 0) setLargeur(w)
    })
    ro.observe(el)
    return () => ro.disconnect()
  }, [vue])

  const series = p.series
  const fmt = (v: number | null) => (v === null ? '-' : p.format ? p.format(v) : formatCompact(v, langue) + (p.unit ? ' ' + p.unit : ''))
  const instants = useMemo(() => Array.from(new Set(series.flatMap((s) => s.points.map((q) => q.t)))).sort((a, b) => a - b), [series])
  const valeurs = series.flatMap((s) => s.points.map((q) => q.v))
  const aMesures = valeurs.some((v) => v !== null)
  const aire = p.aire ?? series.length === 1
  const autres = series.slice(MAX_IDENTITE)

  const H = p.hauteur ?? 200
  // Le seuil entre dans le domaine : jamais dessiné hors du graphique.
  const { min, max, graduations } = echelle(p.seuil === undefined ? valeurs : [...valeurs, p.seuil], p.min, p.max)
  const largeurGraduation = Math.max(...graduations.map((g) => fmt(g).length)) * 7
  const padL = Math.min(96, largeurGraduation + 10)
  const padR = 12
  const padT = 10
  const padB = 24
  const W = Math.max(240, largeur)
  const iw = W - padL - padR
  const ih = H - padT - padB
  const debut = instants[0] ?? 0
  const fin = instants[instants.length - 1] ?? 1
  const etendue = fin - debut
  const xs = (tt: number) => padL + (etendue > 0 ? ((tt - debut) * iw) / etendue : iw / 2)
  const ys = (v: number) => padT + ih - ((v - min) * ih) / (max - min || 1)
  const valeurA = (s: SerieMesure, tt: number) => s.points.find((q) => q.t === tt)?.v ?? null

  const choisir = (x: number) => {
    if (!instants.length) return
    const tt = debut + ((x - padL) / iw) * etendue
    let meilleur = 0
    for (let i = 1; i < instants.length; i++) if (Math.abs(instants[i] - tt) < Math.abs(instants[meilleur] - tt)) meilleur = i
    setCurseur(meilleur)
  }
  const survol = (e: PointerEvent<SVGRectElement>) => {
    const r = (e.currentTarget.ownerSVGElement ?? e.currentTarget).getBoundingClientRect()
    choisir(r.width ? ((e.clientX - r.left) * W) / r.width : padL)
  }
  const clavier = (e: KeyboardEvent<SVGSVGElement>) => {
    if (!instants.length) return
    const dernier = instants.length - 1
    if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
      e.preventDefault()
      const pas = e.key === 'ArrowLeft' ? -1 : 1
      setCurseur((c) => Math.max(0, Math.min(dernier, (c ?? dernier) + pas)))
    } else if (e.key === 'Home') setCurseur(0)
    else if (e.key === 'End') setCurseur(dernier)
    else if (e.key === 'Escape') setCurseur(null)
  }

  const granularite = p.granularite ?? 'instant'
  const cibleX = Math.max(2, Math.floor(iw / 110))
  // Jours et mois : graduations prises parmi les points (une par jour ou par mois, la dernière comprise).
  const graduationsX = granularite === 'instant' ? graduationsTemps(debut, fin, cibleX) : graduationsPoints(instants, cibleX)
  const tc = curseur !== null ? instants[curseur] : null

  const graphique = (
    <svg
      viewBox={'0 0 ' + W + ' ' + H}
      height={H}
      role="img"
      tabIndex={0}
      aria-label={t('graphique.description', { titre: p.title })}
      onKeyDown={clavier}
      onFocus={() => setCurseur((c) => c ?? (instants.length ? instants.length - 1 : null))}
      onBlur={() => setCurseur(null)}
    >
      {graduations.map((g) => (
        <g key={'y' + g}>
          <line className="gm-chart-grid" x1={padL} x2={W - padR} y1={ys(g)} y2={ys(g)} />
          <text className="gm-chart-tick" x={padL - 6} y={ys(g) + 4} textAnchor="end">{fmt(g)}</text>
        </g>
      ))}
      {graduationsX.map((g) => (
        <text key={'x' + g} className="gm-chart-tick" x={xs(g)} y={H - 6} textAnchor="middle">{libelleGraduation(g, granularite, etendue, langue)}</text>
      ))}
      {p.seuil !== undefined ? (
        <g>
          <line className="gm-chart-threshold" x1={padL} x2={W - padR} y1={ys(p.seuil)} y2={ys(p.seuil)} />
          <text className="gm-chart-tick" x={W - padR} y={ys(p.seuil) - 4} textAnchor="end">{t('commun.seuil', { valeur: fmt(p.seuil) })}</text>
        </g>
      ) : null}
      {series.map((s, i) => {
        const pts = s.points.map((q) => ({ x: xs(q.t), y: q.v === null ? null : ys(q.v) }))
        let dernier = s.points.length - 1
        while (dernier >= 0 && s.points[dernier].v === null) dernier--
        return (
          <g key={s.nom} className={classeSerie(i)}>
            {aire && i < MAX_IDENTITE ? <path className="gm-serie-aire" d={cheminAire(pts, padT + ih)} /> : null}
            <path className="gm-serie-trait" d={cheminCourbe(pts)} />
            {dernier >= 0 && i < MAX_IDENTITE ? <circle className="gm-serie-point" cx={xs(s.points[dernier].t)} cy={ys(s.points[dernier].v as number)} r={4} /> : null}
          </g>
        )
      })}
      {tc !== null ? (
        <g>
          <line className="gm-graphique-reticule" x1={xs(tc)} x2={xs(tc)} y1={padT} y2={padT + ih} />
          {series.map((s, i) => {
            const v = valeurA(s, tc)
            return v === null ? null : <circle key={s.nom} className={'gm-serie-point ' + classeSerie(i)} cx={xs(tc)} cy={ys(v)} r={4} />
          })}
        </g>
      ) : null}
      <rect x={padL} y={0} width={iw} height={H} fill="transparent" onPointerMove={survol} onPointerLeave={() => setCurseur(null)} data-testid="zone-survol" />
    </svg>
  )

  const infobulle =
    tc !== null && vue === 'graphique' ? (
      <div className="gm-graphique-infobulle" role="status" style={xs(tc) > W - 200 ? { right: W - xs(tc) + 12, top: 0 } : { left: xs(tc) + 12, top: 0 }}>
        <span>{libellePeriode(tc, granularite, langue)}</span>
        {series.map((s, i) => (
          <span key={s.nom} className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
            <span className={'gm-serie-cle ' + classeSerie(i)} />
            {s.nom}
            <strong className="gm-graphique-valeur">{fmt(valeurA(s, tc))}</strong>
          </span>
        ))}
      </div>
    ) : null

  const tableau = (
    <div style={{ overflowX: 'auto' }}>
      <table className="gm-table gm-table-compact">
        <thead>
          <tr>
            <th scope="col">{t(granularite === 'instant' ? 'graphique.instant' : `graphique.${granularite}`)}</th>
            {series.map((s) => (
              <th key={s.nom} scope="col" className="gm-num">{s.nom}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {instants.map((tt) => (
            <tr key={tt}>
              <td>{libellePeriode(tt, granularite, langue)}</td>
              {series.map((s) => (
                <td key={s.nom} className="gm-num">{fmt(valeurA(s, tt))}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )

  let corps: ReactNode
  if (p.chargement && !aMesures) corps = <div className="gm-graphique-vide" role="status">{t('graphique.chargement')}</div>
  else if (!aMesures) corps = <div className="gm-graphique-vide">{p.vide ?? t('graphique.aucuneMesure')}</div>
  else corps = vue === 'tableau' ? tableau : graphique

  return (
    <section className="gm-card gm-chartcard" aria-label={p.title} aria-busy={p.chargement || p.rechargement || undefined}>
      <header className="gm-card-header">
        <div>
          <h2 className="gm-card-title">{p.title}</h2>
          {p.subtitle ? <div className="gm-field-help">{p.subtitle}</div> : null}
        </div>
        <div className="gm-row">
          {p.status ? <StatusBadge status={p.status} label={p.statusLabel || ''} /> : null}
          {p.actions}
          <Tabs items={[{ key: 'graphique', label: t('graphique.graphique') }, { key: 'tableau', label: t('graphique.tableau') }]} value={vue} onChange={setVue} />
          {p.onExporter ? <IconButton variant="ghost" size="sm" icon="download" label={t('graphique.exporter')} onClick={p.onExporter} /> : null}
        </div>
      </header>
      <div className="gm-card-body">
        <div ref={cadre} className={cx('gm-graphique-cadre', p.rechargement && 'gm-rechargement')}>
          {corps}
          {infobulle}
        </div>
      </div>
      {series.length > 1 ? (
        <footer className="gm-chart-legend">
          {series.slice(0, MAX_IDENTITE).map((s, i) => (
            <span key={s.nom} className="gm-row" style={{ gap: 6 }}>
              <span className={'gm-serie-cle ' + classeSerie(i)} />
              {s.nom}
            </span>
          ))}
          {autres.length ? (
            <span className="gm-row" style={{ gap: 6 }}>
              <span className="gm-serie-cle gm-serie-autre" />
              {t('graphique.autres', { nombre: autres.length })}
            </span>
          ) : null}
        </footer>
      ) : null}
    </section>
  )
}
