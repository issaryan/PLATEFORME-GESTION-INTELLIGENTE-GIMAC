import { memo, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Handle, MarkerType, Position, type Edge, type Node, type NodeProps } from '@xyflow/react'
import { cx } from '@gimac/design-system'
import { CanevasGraphe } from '../../../composants/graphe/CanevasGraphe'
import type { EtatEtape } from '../../../modele/processus'

// Graphe d'un processus d'unité (D-87 §4) sur le canevas générique partagé avec le modélisateur D-77 : une carte par étape
// (intervenant, livrables, signatures, délai), les prédécesseurs en arcs. Modélisateur : cartes déplaçables et reliables ;
// suivi d'une instance : lecture seule, carte colorée par l'état de l'étape.

export type CarteEtape = {
  code: string
  libelle: string
  predecesseurs: string[]
  position: { x: number; y: number } | null
  /** Lignes de résumé (intervenant, livrables et signatures, délai ou échéance). */
  lignes: string[]
  etat?: EtatEtape
  anomalies?: number
}
type DonneesCarte = { carte: CarteEtape; lecture: boolean; surligne: boolean; choisie: boolean; ouvrir: (code: string) => void; libelleOuvrir: string; libelleEtat?: string }
type NoeudEtape = Node<DonneesCarte, 'etape'>

export const LARGEUR_CARTE = 240
export const HAUTEUR_CARTE = 136

const Carte = memo(function Carte({ data }: NodeProps<NoeudEtape>) {
  const { t } = useTranslation()
  const c = data.carte
  return (
    <article className={cx('gm-proc-carte', c.etat && `gm-proc-etat-${c.etat}`, (c.anomalies ?? 0) > 0 && 'gm-proc-carte-anomalie', data.surligne && 'gm-proc-carte-surlignee', data.choisie && 'gm-proc-choisie')}
      aria-label={t('proc.carte.aria', { code: c.code, libelle: c.libelle })} data-etape={c.code}>
      {/* Poignées de liaison décoratives (sans nom : un nom sans rôle est proscrit) ; au clavier, prédécesseurs dans l'onglet Chronologie. */}
      <Handle type="target" position={Position.Left} isConnectable={!data.lecture} />
      <header className="gm-proc-carte-tete">
        <span className="gm-proc-carte-code">{c.code}</span>
        {data.libelleEtat ? <span className="gm-mod-pastille">{data.libelleEtat}</span> : null}
      </header>
      <p className="gm-proc-carte-libelle" title={c.libelle}>{c.libelle || t('proc.etape.sansLibelle')}</p>
      {c.lignes.map((l, i) => <p key={i} className="gm-proc-carte-ligne" title={l}>{l}</p>)}
      {c.anomalies ? <p className="gm-mod-carte-anomalies">{t('proc.carte.anomalies', { count: c.anomalies })}</p> : null}
      <button type="button" className="nodrag gm-proc-carte-ouvrir" onClick={() => data.ouvrir(c.code)} aria-label={`${data.libelleOuvrir} ${c.code}`}>{data.libelleOuvrir}</button>
      <Handle type="source" position={Position.Right} isConnectable={!data.lecture} />
    </article>
  )
})

const TYPES = { etape: Carte }
const CARTES = ['etape']

/** Position d'une étape : la sienne, sinon une disposition en couches (profondeur dans le graphe). */
export function positionsAuto(cartes: Pick<CarteEtape, 'code' | 'predecesseurs' | 'position'>[]): Map<string, { x: number; y: number }> {
  const profondeur = new Map<string, number>()
  const calcul = (c: string, pile: string[] = []): number => {
    if (profondeur.has(c)) return profondeur.get(c)!
    if (pile.includes(c)) return 0
    const e = cartes.find((x) => x.code === c)
    const p = e && e.predecesseurs.length ? Math.max(...e.predecesseurs.filter((x) => cartes.some((y) => y.code === x)).map((x) => calcul(x, [...pile, c]) + 1), 0) : 0
    profondeur.set(c, p)
    return p
  }
  const parColonne = new Map<number, number>()
  return new Map(cartes.map((c) => {
    if (c.position) return [c.code, c.position]
    const col = calcul(c.code)
    const rang = parColonne.get(col) ?? 0
    parColonne.set(col, rang + 1)
    return [c.code, { x: col * (LARGEUR_CARTE + 60), y: 40 + rang * (HAUTEUR_CARTE + 40) }]
  }))
}

export function CanevasProcessus(p: {
  cartes: CarteEtape[]; lecture: boolean; surlignes?: Set<string>; miniCarte?: boolean; choisie?: string | null; libelleOuvrir: string; libelleEtat?: (e: EtatEtape) => string; testId: string
  onOuvrir: (code: string) => void; onRelier?: (de: string, vers: string) => void; onDelier?: (de: string, vers: string) => void; onPlacer?: (l: { code: string; x: number; y: number }[]) => void
}) {
  const { t } = useTranslation()
  const { cartes, lecture, surlignes, choisie, libelleOuvrir, libelleEtat, onOuvrir } = p
  const noeuds = useMemo<NoeudEtape[]>(() => {
    const positions = positionsAuto(cartes)
    return cartes.map((c) => ({
      id: c.code, type: 'etape', position: positions.get(c.code)!, width: LARGEUR_CARTE, height: HAUTEUR_CARTE, draggable: !lecture, deletable: false, connectable: !lecture,
      data: { carte: c, lecture, surligne: !!surlignes?.has(c.code), choisie: choisie === c.code, ouvrir: onOuvrir, libelleOuvrir, libelleEtat: c.etat && libelleEtat ? libelleEtat(c.etat) : undefined },
      ariaLabel: `${c.code} · ${c.libelle}`,
    }))
  }, [cartes, lecture, surlignes, choisie, onOuvrir, libelleOuvrir, libelleEtat])
  const arcs = useMemo<Edge[]>(() => cartes.flatMap((c) => c.predecesseurs.filter((d) => cartes.some((x) => x.code === d)).map((d) => ({
    id: `${d}>${c.code}`, source: d, target: c.code, type: 'smoothstep', deletable: !lecture, focusable: true, className: 'gm-mod-arc', markerEnd: { type: MarkerType.ArrowClosed },
    ariaLabel: t('proc.arc.aria', { de: d, vers: c.code }),
  }))), [cartes, lecture, t])
  return (
    <CanevasGraphe<NoeudEtape> noeuds={noeuds} arcs={arcs} types={TYPES as never} lecture={lecture} typesCartes={CARTES} prefixeAria="proc.canevas" testId={p.testId}
      onOuvrir={onOuvrir} onRelier={p.onRelier} onDelier={p.onDelier} onPlacer={p.onPlacer} miniCarte={p.miniCarte} />
  )
}
