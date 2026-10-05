import { memo, useCallback, useEffect, useMemo, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Background, Controls, Handle, MarkerType, MiniMap, Position, ReactFlow, useNodesState, type Connection, type Edge, type Node, type NodeProps,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import './modelisateur.css'
import { cx } from '@gimac/design-system'
import type { CatalogueModelisateur, GrapheReferentiel, NoeudGraphe, PositionNoeud } from '../../../modele/homologation'
import { CARTE, couloirs } from '../../../adaptateurs/modelisateur'

// Canevas du modélisateur sur @xyflow/react (MIT) : couloirs par étape (colonnes) et par segment (bandes),
// une carte par opération, les prédécesseurs en arcs. Tirer d'une carte à l'autre crée un arc, supprimer un arc
// (touche Suppr) le retire, déplacer une carte enregistre sa position après un court regroupement.

export type DonneesCarte = { noeud: NoeudGraphe; lecture: boolean; surligne: boolean; ouvrir: (code: string) => void }
export type DonneesCouloir = { libelle: string; largeur: number; hauteur: number; sens: 'etape' | 'segment' }
type NoeudCarte = Node<DonneesCarte, 'operation'>
type NoeudCouloir = Node<DonneesCouloir, 'couloir'>
type NoeudFlux = NoeudCarte | NoeudCouloir

/** Délai de regroupement des positions avant PUT /positions (ms). */
export const DELAI_POSITIONS = 400

const Carte = memo(function Carte({ data }: NodeProps<NoeudCarte>) {
  const { t } = useTranslation()
  const n = data.noeud
  const fonction = (f: string | null) => (f ? t(`parcours.fonction.${f}`, { defaultValue: f }) : t('modelisateur.aucun'))
  const anomalies = n.anomalies ?? []
  return (
    <article
      className={cx('gm-mod-carte', n.critique && 'gm-mod-carte-critique', anomalies.length > 0 && 'gm-mod-carte-anomalie', data.surligne && 'gm-mod-carte-surlignee')}
      aria-label={t('modelisateur.carte.aria', { code: n.code, libelle: n.libelle })}
      data-code={n.code}
    >
      <Handle type="target" position={Position.Left} isConnectable={!data.lecture} aria-label={t('modelisateur.carte.entree', { code: n.code })} />
      <header className="gm-mod-carte-tete">
        <span className="gm-mono gm-mod-carte-code">{n.code}</span>
        <span className="gm-mod-pastilles">
          {n.jalon ? <span className="gm-mod-pastille gm-mod-pastille-jalon" title={t(`modelisateur.jalon.${n.jalon}`)}>{t(`modelisateur.jalon.${n.jalon}`)}</span> : null}
          {n.declencheur ? <span className="gm-mod-pastille gm-mod-pastille-evenement" title={n.declencheur}>{t('modelisateur.carte.evenement')}</span> : null}
          {n.particularite ? <span className="gm-mod-pastille" title={t(`modelisateur.particularite.${n.particularite}`)}>{t(`modelisateur.particularite.${n.particularite}`)}</span> : null}
        </span>
      </header>
      <p className="gm-mod-carte-libelle" title={n.libelle}>{n.libelle}</p>
      <p className="gm-mod-carte-ligne">
        <abbr title={t('parcours.qualite.R')}>R</abbr> {fonction(n.responsable)} · <abbr title={t('parcours.qualite.V')}>V</abbr> {fonction(n.validateur)}
      </p>
      <p className="gm-mod-carte-ligne">
        {t('modelisateur.carte.livrables', { count: n.livrables?.length ?? 0 })} · {t('modelisateur.carte.delai', { n: n.delaiJours ?? 0 })}
        {n.critique ? <span className="gm-mod-critique-marque"> · {t('modelisateur.carte.critique')}</span> : null}
      </p>
      {anomalies.length ? (
        <p className="gm-mod-carte-anomalies" title={anomalies.map((a) => t(`modelisateur.anomalie.${a}`)).join(' ; ')}>
          {t('modelisateur.carte.anomalies', { count: anomalies.length, premiere: t(`modelisateur.anomalie.${anomalies[0]}`) })}
        </p>
      ) : null}
      <button type="button" className="nodrag gm-mod-carte-modifier" onClick={() => data.ouvrir(n.code)} aria-label={t(data.lecture ? 'modelisateur.action.consulterOp' : 'modelisateur.action.modifierOp', { code: n.code })}>
        {data.lecture ? t('modelisateur.action.consulter') : t('modelisateur.action.modifier')}
      </button>
      <Handle type="source" position={Position.Right} isConnectable={!data.lecture} aria-label={t('modelisateur.carte.sortie', { code: n.code })} />
    </article>
  )
})

function Couloir({ data }: NodeProps<NoeudCouloir>) {
  return (
    <div className={cx('gm-mod-couloir', `gm-mod-couloir-${data.sens}`)} style={{ width: data.largeur, height: data.hauteur }} aria-hidden="true">
      <span className="gm-mod-couloir-titre">{data.libelle}</span>
    </div>
  )
}

const TYPES = { operation: Carte, couloir: Couloir }

export type CanevasProps = {
  graphe: GrapheReferentiel
  catalogue: CatalogueModelisateur
  positions: Map<string, PositionNoeud>
  lecture: boolean
  surlignes: Set<string>
  onOuvrir: (code: string) => void
  onRelier: (de: string, vers: string) => void
  onDelier: (de: string, vers: string) => void
  onPlacer: (positions: { code: string; x: number; y: number }[]) => void
}

export function Canevas(p: CanevasProps) {
  const { t } = useTranslation()
  const { onPlacer, onOuvrir, onRelier, onDelier } = p
  const calcules = useMemo<NoeudFlux[]>(() => {
    const { etapes, segments } = couloirs(p.graphe.noeuds, p.positions, p.catalogue, t)
    const fond = (c: (typeof etapes)[number], sens: 'etape' | 'segment', z: number): NoeudCouloir => ({
      id: c.cle, type: 'couloir', position: { x: c.x, y: c.y }, data: { libelle: c.libelle, largeur: c.largeur, hauteur: c.hauteur, sens },
      draggable: false, selectable: false, connectable: false, focusable: false, deletable: false, zIndex: z, className: 'gm-mod-couloir-noeud',
    })
    return [
      ...segments.map((c) => fond(c, 'segment', -2)),
      ...etapes.map((c) => fond(c, 'etape', -1)),
      ...p.graphe.noeuds.map((n): NoeudCarte => ({
        id: n.code, type: 'operation', position: p.positions.get(n.code) ?? { x: 0, y: 0 }, width: CARTE.largeur, height: CARTE.hauteur,
        data: { noeud: n, lecture: p.lecture, surligne: p.surlignes.has(n.code), ouvrir: onOuvrir }, draggable: !p.lecture, deletable: false, connectable: !p.lecture,
        ariaLabel: `${n.code} · ${n.libelle}`,
      })),
    ]
  }, [p.graphe.noeuds, p.positions, p.catalogue, p.lecture, p.surlignes, onOuvrir, t])
  const [noeuds, setNoeuds, onNodesChange] = useNodesState<NoeudFlux>(calcules)
  useEffect(() => setNoeuds(calcules), [calcules, setNoeuds])

  const arcs = useMemo<Edge[]>(
    () =>
      p.graphe.arcs.map((a) => ({
        id: `${a.de}>${a.vers}`, source: a.de, target: a.vers, type: 'smoothstep', deletable: !p.lecture, focusable: true,
        className: cx('gm-mod-arc', a.critique && 'gm-mod-arc-critique'), markerEnd: { type: MarkerType.ArrowClosed },
        ariaLabel: t('modelisateur.arc.aria', { de: a.de, vers: a.vers }),
      })),
    [p.graphe.arcs, p.lecture, t],
  )

  // Positions regroupées : un seul PUT /positions après une rafale de déplacements.
  const enAttente = useRef(new Map<string, { code: string; x: number; y: number }>())
  const minuteur = useRef<number | undefined>(undefined)
  useEffect(() => () => window.clearTimeout(minuteur.current), [])
  const deplace = useCallback(
    (_: unknown, __: unknown, deplaces: Node[]) => {
      deplaces.filter((n) => n.type === 'operation').forEach((n) => enAttente.current.set(n.id, { code: n.id, x: Math.round(n.position.x), y: Math.round(n.position.y) }))
      window.clearTimeout(minuteur.current)
      minuteur.current = window.setTimeout(() => {
        const l = [...enAttente.current.values()]
        enAttente.current.clear()
        if (l.length) onPlacer(l)
      }, DELAI_POSITIONS)
    },
    [onPlacer],
  )

  return (
    <div className="gm-mod-canevas" data-testid="modelisateur-canevas">
      <ReactFlow<NoeudFlux, Edge>
        nodes={noeuds}
        edges={arcs}
        nodeTypes={TYPES}
        onNodesChange={onNodesChange}
        onNodeDragStop={deplace}
        onNodeDoubleClick={(_, n) => (n.type === 'operation' ? onOuvrir(n.id) : undefined)}
        onConnect={(c: Connection) => (c.source && c.target && c.source !== c.target ? onRelier(c.source, c.target) : undefined)}
        isValidConnection={(c) => c.source !== c.target}
        onBeforeDelete={async ({ edges }) => ({ nodes: [], edges })}
        onEdgesDelete={(l) => l.forEach((e) => onDelier(e.source, e.target))}
        nodesDraggable={!p.lecture}
        nodesConnectable={!p.lecture}
        deleteKeyCode={p.lecture ? null : ['Delete', 'Backspace']}
        fitView
        fitViewOptions={{ padding: 0.1 }}
        minZoom={0.15}
        maxZoom={2}
        proOptions={{ hideAttribution: true }}
        ariaLabelConfig={{
          'node.a11yDescription.default': t('modelisateur.canevas.aideNoeud'),
          'node.a11yDescription.keyboardDisabled': t('modelisateur.canevas.aideNoeud'),
          'node.a11yDescription.ariaLiveMessage': ({ direction, x, y }) => t('modelisateur.canevas.deplace', { direction, x, y }),
          'handle.ariaLabel': t('modelisateur.canevas.poignee'),
          'edge.a11yDescription.default': t('modelisateur.canevas.aideArc'),
          'controls.ariaLabel': t('modelisateur.canevas.controles'),
          'controls.zoomIn.ariaLabel': t('modelisateur.canevas.zoomAvant'),
          'controls.zoomOut.ariaLabel': t('modelisateur.canevas.zoomArriere'),
          'controls.fitView.ariaLabel': t('modelisateur.canevas.ajuster'),
          'controls.interactive.ariaLabel': t('modelisateur.canevas.verrou'),
          'minimap.ariaLabel': t('modelisateur.canevas.miniCarte'),
        }}
      >
        <Background gap={24} />
        <Controls showInteractive={false} />
        <MiniMap pannable zoomable nodeClassName={(n) => (n.type === 'couloir' ? 'gm-mod-mini-couloir' : 'gm-mod-mini-carte')} />
      </ReactFlow>
    </div>
  )
}
