import { cheminAire, cheminCourbe, classeSerie, echelle, formatCompact } from '../../lib/graphiques'
import { useLangue, useLibelle } from '../../i18n/LangueProvider'

export interface SparklineProps {
  /** Valeurs dans l'ordre du temps ; null = mesure absente (trou dans la courbe, jamais zéro). */
  points: (number | null)[]
  /** Nom de la mesure, pour l'étiquette accessible (« Processeur : de 12 à 34 % »). */
  label: string
  unit?: string
  /** Rang de série (0 à 3, ordre fixe validé) ; « autre » pour une courbe de contexte désaccentuée. */
  serie?: number | 'autre'
  /** Lavis d'aire sous la courbe (10 %). */
  aire?: boolean
  min?: number
  max?: number
  hauteur?: number
}

// Mini-courbe (tuile d'indicateur, ligne de tableau) : sans axe ni grille, trait de 2 px, point final
// cerclé de la surface, lavis facultatif. Largeur fluide (viewBox étiré), hauteur fixe. La valeur
// courante et la tendance restent lisibles hors du graphique (tuile, colonne voisine, étiquette).
export function Sparkline(p: SparklineProps) {
  const t = useLibelle()
  const langue = useLangue()
  const W = 120
  const H = p.hauteur ?? 32
  const pad = 4
  const valeurs = p.points
  const mesures = valeurs.filter((v): v is number => v !== null)
  const classe = p.serie === 'autre' ? 'gm-serie-autre' : classeSerie(p.serie ?? 0)
  if (mesures.length === 0) {
    return (
      <svg className="gm-sparkline" viewBox={'0 0 ' + W + ' ' + H} preserveAspectRatio="none" role="img" aria-label={t('graphique.sansMesure', { libelle: p.label })} height={H} />
    )
  }
  const { min, max } = echelle(valeurs, p.min ?? Math.min(...mesures), p.max ?? Math.max(...mesures))
  const n = valeurs.length
  const xs = (i: number) => (n > 1 ? (i * W) / (n - 1) : W / 2)
  const ys = (v: number) => (max === min ? H / 2 : pad + (H - 2 * pad) * (1 - (v - min) / (max - min)))
  const pts = valeurs.map((v, i) => ({ x: xs(i), y: v === null ? null : ys(v) }))
  let dernier = n - 1
  while (dernier >= 0 && valeurs[dernier] === null) dernier--
  const suffixe = p.unit ? ' ' + p.unit : ''
  const premiere = mesures[0]
  const finale = mesures[mesures.length - 1]
  const resume = t('graphique.resumeSerie', { libelle: p.label, debut: formatCompact(premiere, langue) + suffixe, fin: formatCompact(finale, langue) + suffixe })
  // Le tracé s'étire avec la largeur ; le point final est dans une couche non étirée (coordonnées en %),
  // pour rester un disque quelle que soit la largeur de la tuile.
  return (
    <span className={'gm-sparkline-cadre ' + classe} role="img" aria-label={resume} style={{ height: H }}>
      <svg className="gm-sparkline" viewBox={'0 0 ' + W + ' ' + H} preserveAspectRatio="none" height={H} aria-hidden="true">
        {p.aire ? <path className="gm-serie-aire" d={cheminAire(pts, H)} /> : null}
        <path className="gm-serie-trait" d={cheminCourbe(pts)} vectorEffect="non-scaling-stroke" />
      </svg>
      <svg className="gm-sparkline-point" height={H} aria-hidden="true">
        <circle className="gm-serie-point" cx={((xs(dernier) / W) * 100).toFixed(2) + '%'} cy={ys(valeurs[dernier] as number)} r={3.5} />
      </svg>
    </span>
  )
}
