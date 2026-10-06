import { useTranslation } from 'react-i18next'
import { RadioGroup, SerieTemporelle, Sparkline, libelleInstant } from '@gimac/design-system'
import { useSeries } from '../hooks'
import { PERIODES, type MetriqueSerie, type PeriodeSerie, type SeriesMetriques } from '../modele/plateforme'

// Graphiques de la supervision (centre de contrôle) : courbes des métriques historisées servies par la
// passerelle (/api/plateforme/series). Les tableaux de l'état instantané restent en place ; les graphiques
// les complètent. La période se choisit une fois, au-dessus des graphiques, et s'applique à tous.

/** Sélecteur de période, en tête de la zone qu'il gouverne. */
export function SelecteurPeriode({ valeur, onChange }: { valeur: PeriodeSerie; onChange: (p: PeriodeSerie) => void }) {
  const { t } = useTranslation()
  return (
    <RadioGroup
      name="periode-supervision"
      label={t('graphiques.periode')}
      value={valeur}
      options={PERIODES.map((p) => ({ value: p, label: t(`graphiques.periode.${p}`) }))}
      onChange={(v) => onChange(v as PeriodeSerie)}
    />
  )
}

/** Fichier CSV d'une réponse : une ligne par instant, une colonne par série (mesure absente : vide). */
export function versCsv(r: SeriesMetriques, langue: string): string {
  const instants = Array.from(new Set(r.series.flatMap((s) => s.points.map((p) => p.t)))).sort((a, b) => a - b)
  const echapper = (x: string) => (/[;"\n]/.test(x) ? `"${x.replace(/"/g, '""')}"` : x)
  const lignes = [['instant', ...r.series.map((s) => s.nom)].map(echapper).join(';')]
  for (const t of instants) {
    lignes.push([libelleInstant(t, langue), ...r.series.map((s) => String(s.points.find((p) => p.t === t)?.v ?? ''))].map(echapper).join(';'))
  }
  return lignes.join('\n')
}

function telecharger(nom: string, contenu: string) {
  const url = URL.createObjectURL(new Blob([contenu], { type: 'text/csv;charset=utf-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = nom
  a.click()
  URL.revokeObjectURL(url)
}

export interface GraphiqueMetriqueProps {
  metrique: MetriqueSerie
  periode: PeriodeSerie
  /** Restreint la métrique à un service, un conteneur ou un hôte. */
  cible?: string
  min?: number
  max?: number
  seuil?: number
  /** Titre à la place du titre de la métrique (fiche d'un service). */
  titre?: string
}

/** Courbe d'une métrique historisée, avec état de chargement, mesures indisponibles et export CSV. */
export function GraphiqueMetrique(p: GraphiqueMetriqueProps) {
  const { t, i18n } = useTranslation()
  const r = useSeries(p.metrique, p.periode, p.cible)
  const d = r.data
  const conteneurs = p.metrique === 'PLATEFORME_CPU' || p.metrique.startsWith('CONTENEUR_')
  const vide = d && !d.disponible ? t('graphiques.indisponible') : conteneurs ? t('graphiques.conteneursAbsents') : t('graphiques.aucuneMesure')
  return (
    <SerieTemporelle
      title={p.titre ?? t(`graphiques.metrique.${p.metrique}`)}
      subtitle={t(`graphiques.aide.${p.metrique}`) + ' · ' + t(`graphiques.periode.${p.periode}`)}
      series={d?.series ?? []}
      unit={d?.unite}
      min={p.min}
      max={p.max}
      seuil={p.seuil}
      chargement={r.isPending}
      rechargement={r.isFetching && !r.isPending}
      vide={r.isError ? t('graphiques.indisponible') : vide}
      onExporter={d && d.series.length ? () => telecharger(`${p.metrique.toLowerCase()}${p.cible ? '-' + p.cible : ''}.csv`, versCsv(d, i18n.language)) : undefined}
    />
  )
}

/** Mini-courbe d'une série d'une métrique (colonne de tableau) : la série nommée `nom`, sinon rien. */
export function TendanceMetrique({ series, nom, libelle }: { series: SeriesMetriques | undefined; nom: string; libelle: string }) {
  const s = series?.series.find((x) => x.nom === nom)
  if (!s || !series) return <span className="gm-field-help">—</span>
  return (
    <span style={{ display: 'block', minWidth: 96 }}>
      <Sparkline points={s.points.map((p) => p.v)} label={libelle} unit={series.unite} hauteur={24} />
    </span>
  )
}

/**
 * Graphiques du centre de contrôle : charge de la plateforme, puis services (latence, débit, erreurs, tas),
 * puis serveurs (hôtes et conteneurs). Les seuils sont ceux des alertes de la plateforme.
 */
export function GraphiquesPlateforme({ periode }: { periode: PeriodeSerie }) {
  const { t } = useTranslation()
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-6)' }}>
      <GraphiqueMetrique metrique="PLATEFORME_CPU" periode={periode} />
      <section className="gm-col" aria-label={t('graphiques.groupe.services')}>
        <h3 className="gm-type-h3" style={{ margin: 0 }}>{t('graphiques.groupe.services')}</h3>
        <div className="gm-graphiques-grille">
          <GraphiqueMetrique metrique="SERVICE_LATENCE_P95" periode={periode} />
          <GraphiqueMetrique metrique="SERVICE_DEBIT" periode={periode} />
          <GraphiqueMetrique metrique="SERVICE_ERREURS" periode={periode} seuil={1} />
          <GraphiqueMetrique metrique="SERVICE_TAS_JVM" periode={periode} />
        </div>
      </section>
      <section className="gm-col" aria-label={t('graphiques.groupe.serveurs')}>
        <h3 className="gm-type-h3" style={{ margin: 0 }}>{t('graphiques.groupe.serveurs')}</h3>
        <div className="gm-graphiques-grille">
          <GraphiqueMetrique metrique="HOTE_CPU" periode={periode} min={0} max={100} seuil={85} />
          <GraphiqueMetrique metrique="HOTE_MEMOIRE" periode={periode} min={0} max={100} seuil={90} />
          <GraphiqueMetrique metrique="HOTE_DISQUE" periode={periode} min={0} max={100} seuil={85} />
          <GraphiqueMetrique metrique="CONTENEUR_MEMOIRE" periode={periode} />
        </div>
      </section>
    </div>
  )
}
