import { useCallback, useEffect, useRef, type ComponentType } from 'react'
import { useTranslation } from 'react-i18next'
import { Background, Controls, MiniMap, ReactFlow, useNodesInitialized, useNodesState, useReactFlow, type Connection, type Edge, type Node, type NodeProps } from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import '../../pages/des/modelisateur/modelisateur.css'

// Canevas de graphe générique sur @xyflow/react (MIT), partagé par le modélisateur des référentiels (D-77) et celui des
// processus d'unité (D-87) : nœuds fournis par l'appelant (cartes et fonds), arcs de prédécesseurs, habillage par les
// seuls jetons --gm-* (mode clair et sombre), libellés d'accessibilité traduits. Tirer d'une carte à l'autre crée un
// arc, supprimer un arc (touche Suppr) le retire, déplacer des cartes rend leurs positions regroupées après un délai.

/** Délai de regroupement des positions (ms) avant leur enregistrement. */
export const DELAI_POSITIONS = 400

export type CanevasGrapheProps<N extends Node> = {
  noeuds: N[]
  arcs: Edge[]
  // Composants des types de nœuds de l'appelant (cartes et fonds).
  types: Record<string, ComponentType<NodeProps<N>>>
  lecture: boolean
  /** Types de nœuds qui sont des cartes : ouverts au double-clic, positions rendues après un déplacement. */
  typesCartes: string[]
  /** Préfixe des libellés d'accessibilité (aideNoeud, aideArc, deplace, poignee, controles, zoomAvant, zoomArriere, ajuster, verrou, miniCarte). */
  prefixeAria: string
  testId: string
  onOuvrir: (id: string) => void
  onRelier?: (de: string, vers: string) => void
  onDelier?: (de: string, vers: string) => void
  onPlacer?: (positions: { code: string; x: number; y: number }[]) => void
  classeMiniCarte?: (n: Node) => string
  /** Mini-carte (par défaut) ; inutile pour un petit graphe en lecture. */
  miniCarte?: boolean
}

/**
 * Cadrage : à l'ouverture et chaque fois que l'ensemble des cartes change (autre version, lecture filtrée avec
 * fantômes, D-89), la vue est ajustée sur toutes les cartes une fois leurs dimensions connues.
 */
/** Zoom au-dessous duquel les cartes ne se lisent plus (texte de 12 px réduit sous 8 px). */
const ZOOM_LISIBLE = 0.65
const MARGE_CADRAGE = 24
function Cadrage({ cle }: { cle: string }) {
  const flux = useReactFlow()
  const prets = useNodesInitialized()
  const fait = useRef('')
  useEffect(() => {
    if (!prets || !cle || fait.current === cle) return
    fait.current = cle
    // Cadrage sur les cartes seules (opérations et fantômes), sans les couloirs des étapes vides. Recette HOM-18 : un grand
    // graphe n'est jamais réduit sous un zoom lisible ; la vue s'ouvre alors sur le début du parcours (en haut à gauche).
    const ids = cle.split('|')
    void Promise.resolve(flux.fitView({ nodes: ids.map((id) => ({ id })), padding: 0.15, maxZoom: 1 })).then(() => {
      if (flux.getZoom() >= ZOOM_LISIBLE) return
      const b = flux.getNodesBounds(ids)
      void flux.setViewport({ x: -b.x * ZOOM_LISIBLE + MARGE_CADRAGE, y: -b.y * ZOOM_LISIBLE + MARGE_CADRAGE, zoom: ZOOM_LISIBLE })
    })
  }, [prets, cle, flux])
  return null
}

export function CanevasGraphe<N extends Node>(p: CanevasGrapheProps<N>) {
  const { t } = useTranslation()
  const { onPlacer, onRelier, onDelier, onOuvrir, typesCartes } = p
  const [noeuds, setNoeuds, onNodesChange] = useNodesState<N>(p.noeuds)
  useEffect(() => setNoeuds(p.noeuds), [p.noeuds, setNoeuds])

  // Positions regroupées : un seul enregistrement après une rafale de déplacements.
  const enAttente = useRef(new Map<string, { code: string; x: number; y: number }>())
  const minuteur = useRef<number | undefined>(undefined)
  useEffect(() => () => window.clearTimeout(minuteur.current), [])
  const deplace = useCallback(
    (_: unknown, __: unknown, deplaces: Node[]) => {
      deplaces.filter((n) => typesCartes.includes(n.type ?? '')).forEach((n) => enAttente.current.set(n.id, { code: n.id, x: Math.round(n.position.x), y: Math.round(n.position.y) }))
      window.clearTimeout(minuteur.current)
      minuteur.current = window.setTimeout(() => {
        const l = [...enAttente.current.values()]
        enAttente.current.clear()
        if (l.length) onPlacer?.(l)
      }, DELAI_POSITIONS)
    },
    [onPlacer, typesCartes],
  )
  const a = (cle: string) => t(`${p.prefixeAria}.${cle}`)

  return (
    <div className="gm-mod-canevas" data-testid={p.testId}>
      <ReactFlow<N, Edge>
        nodes={noeuds}
        edges={p.arcs}
        nodeTypes={p.types as never}
        onNodesChange={onNodesChange}
        onNodeDragStop={deplace}
        onNodeDoubleClick={(_, n) => (typesCartes.includes(n.type ?? '') ? onOuvrir(n.id) : undefined)}
        onConnect={(c: Connection) => (c.source && c.target && c.source !== c.target ? onRelier?.(c.source, c.target) : undefined)}
        isValidConnection={(c) => c.source !== c.target}
        onBeforeDelete={async ({ edges }) => ({ nodes: [], edges })}
        onEdgesDelete={(l) => l.forEach((e) => onDelier?.(e.source, e.target))}
        nodesDraggable={!p.lecture}
        nodesConnectable={!p.lecture}
        deleteKeyCode={p.lecture ? null : ['Delete', 'Backspace']}
        // Cadrage initial confié à <Cadrage> (cartes seules, zoom lisible, HOM-18) ; ces options servent au bouton « Ajuster ».
        fitViewOptions={{ padding: 0.1, maxZoom: 1 }}
        minZoom={0.15}
        maxZoom={2}
        proOptions={{ hideAttribution: true }}
        ariaLabelConfig={{
          'node.a11yDescription.default': a('aideNoeud'),
          'node.a11yDescription.keyboardDisabled': a('aideNoeud'),
          'node.a11yDescription.ariaLiveMessage': ({ direction, x, y }) => t(`${p.prefixeAria}.deplace`, { direction, x, y }),
          'handle.ariaLabel': a('poignee'),
          'edge.a11yDescription.default': a('aideArc'),
          'controls.ariaLabel': a('controles'),
          'controls.zoomIn.ariaLabel': a('zoomAvant'),
          'controls.zoomOut.ariaLabel': a('zoomArriere'),
          'controls.fitView.ariaLabel': a('ajuster'),
          'controls.interactive.ariaLabel': a('verrou'),
          'minimap.ariaLabel': a('miniCarte'),
        }}
      >
        <Cadrage cle={p.noeuds.filter((n) => n.type !== 'couloir').map((n) => n.id).sort().join('|')} />
        <Background gap={24} />
        <Controls showInteractive={false} />
        {p.miniCarte === false ? null : <MiniMap pannable zoomable nodeClassName={p.classeMiniCarte ?? (() => 'gm-mod-mini-carte')} />}
      </ReactFlow>
    </div>
  )
}
