import { memo, useMemo, type CSSProperties } from 'react'
import { useTranslation } from 'react-i18next'
import { Handle, MarkerType, Position, type Edge, type Node, type NodeProps } from '@xyflow/react'
import { CanevasGraphe } from '../../../composants/graphe/CanevasGraphe'
import { cx } from '@gimac/design-system'
import type { CatalogueModelisateur, GrapheReferentiel, NoeudGraphe, PositionNoeud } from '../../../modele/homologation'
import { CARTE, couloirs, delaiOperation, type SourceEtapes } from '../../../adaptateurs/modelisateur'

// Canevas du modélisateur sur @xyflow/react (MIT) : couloirs par étape (colonnes) et par segment (bandes),
// une carte par opération, les prédécesseurs en arcs. Tirer d'une carte à l'autre crée un arc, supprimer un arc
// (touche Suppr) le retire, déplacer une carte enregistre sa position après un court regroupement.

export type DonneesCarte = { noeud: NoeudGraphe; lecture: boolean; surligne: boolean; ouvrir: (code: string) => void }
export type DonneesCouloir = { libelle: string; largeur: number; hauteur: number; sens: 'etape' | 'segment'; jeton?: string | null }
export type DonneesFantome = { noeud: NoeudGraphe }
type NoeudCarte = Node<DonneesCarte, 'operation'>
type NoeudCouloir = Node<DonneesCouloir, 'couloir'>
/** Fantôme (D-89) : voisin direct d'une opération qui me concerne ; ni déplacé, ni relié, ni ouvert. */
type NoeudFantome = Node<DonneesFantome, 'fantome'>
type NoeudFlux = NoeudCarte | NoeudCouloir | NoeudFantome

/** Délai de regroupement des positions avant PUT /positions (ms), celui du canevas générique. */
export { DELAI_POSITIONS } from '../../../composants/graphe/CanevasGraphe'
const CARTES = ['operation']

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
      {/* Poignées de liaison décoratives (sans nom : un nom sans rôle est proscrit) ; au clavier, arcs dans la vue liste. */}
      <Handle type="target" position={Position.Left} isConnectable={!data.lecture} />
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
        {t('modelisateur.carte.livrables', { count: n.livrables?.length ?? 0 })} · {delaiOperation(t, n.delaiJours, n.delaiMaxJours)}
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
      <Handle type="source" position={Position.Right} isConnectable={!data.lecture} />
    </article>
  )
})

const CarteFantome = memo(function CarteFantome({ data }: NodeProps<NoeudFantome>) {
  const { t } = useTranslation()
  const n = data.noeud
  return (
    <article className="gm-mod-carte gm-mod-carte-fantome" aria-label={t('modelisateur.fantome.aria', { code: n.code, libelle: n.libelle })} data-code={n.code} data-fantome="true">
      <Handle type="target" position={Position.Left} isConnectable={false} />
      <header className="gm-mod-carte-tete">
        <span className="gm-mono gm-mod-carte-code">{n.code}</span>
        <span className="gm-mod-pastilles"><span className="gm-mod-pastille">{t('modelisateur.fantome')}</span></span>
      </header>
      <p className="gm-mod-carte-libelle" title={n.libelle}>{n.libelle}</p>
      <Handle type="source" position={Position.Right} isConnectable={false} />
    </article>
  )
})

function Couloir({ data }: NodeProps<NoeudCouloir>) {
  // Couleur de l'étape (D-89) : variable CSS du design system servie par le service (jeton).
  const couleur = data.jeton ? ({ '--gm-mod-couloir-couleur': `var(${data.jeton})` } as CSSProperties) : {}
  return (
    <div className={cx('gm-mod-couloir', `gm-mod-couloir-${data.sens}`, data.jeton && 'gm-mod-couloir-colore')} style={{ width: data.largeur, height: data.hauteur, ...couleur }} aria-hidden="true">
      <span className="gm-mod-couloir-titre">{data.libelle}</span>
    </div>
  )
}

const TYPES = { operation: Carte, couloir: Couloir, fantome: CarteFantome }

export type CanevasProps = {
  graphe: GrapheReferentiel
  catalogue: CatalogueModelisateur
  /** Étapes des couloirs (D-89) : celles de la version, sinon le catalogue. */
  etapes: SourceEtapes
  positions: Map<string, PositionNoeud>
  lecture: boolean
  surlignes: Set<string>
  onOuvrir: (code: string) => void
  onRelier: (de: string, vers: string) => void
  onDelier: (de: string, vers: string) => void
  onPlacer: (positions: { code: string; x: number; y: number }[]) => void
  /** Carte déposée dans le couloir d'une autre étape : rattachement de l'opération à cette étape (null : sans étape). */
  onChangerEtape?: (code: string, etape: string | null) => void
}

/** Couloir d'étape sous le centre d'une carte déposée en (x, y) ; undefined hors de tout couloir. */
export function couloirSous(couloirs: { cle: string; x: number; largeur: number }[], x: number): string | null | undefined {
  const centre = x + CARTE.largeur / 2
  const c = couloirs.find((k) => centre >= k.x && centre <= k.x + k.largeur)
  if (!c) return undefined
  const code = c.cle.replace(/^etape-/, '')
  return code === 'aucune' ? null : code
}

export function Canevas(p: CanevasProps) {
  const { t } = useTranslation()
  const { onOuvrir, onRelier, onDelier, onChangerEtape } = p
  const calcules = useMemo<NoeudFlux[]>(() => {
    const { etapes, segments } = couloirs(p.graphe.noeuds, p.positions, p.etapes, t)
    const fond = (c: (typeof etapes)[number], sens: 'etape' | 'segment', z: number): NoeudCouloir => ({
      id: c.cle, type: 'couloir', position: { x: c.x, y: c.y }, data: { libelle: c.libelle, largeur: c.largeur, hauteur: c.hauteur, sens, jeton: sens === 'etape' ? c.jeton : null },
      draggable: false, selectable: false, connectable: false, focusable: false, deletable: false, zIndex: z, className: 'gm-mod-couloir-noeud',
    })
    return [
      ...segments.map((c) => fond(c, 'segment', -2)),
      ...etapes.map((c) => fond(c, 'etape', -1)),
      ...p.graphe.noeuds.filter((n) => n.fantome).map((n): NoeudFantome => ({
        id: n.code, type: 'fantome', position: p.positions.get(n.code) ?? { x: 0, y: 0 }, width: CARTE.largeur, height: CARTE.hauteur, data: { noeud: n },
        draggable: false, deletable: false, connectable: false, selectable: false, ariaLabel: t('modelisateur.fantome.aria', { code: n.code, libelle: n.libelle }),
      })),
      ...p.graphe.noeuds.filter((n) => !n.fantome).map((n): NoeudCarte => ({
        id: n.code, type: 'operation', position: p.positions.get(n.code) ?? { x: 0, y: 0 }, width: CARTE.largeur, height: CARTE.hauteur,
        data: { noeud: n, lecture: p.lecture, surligne: p.surlignes.has(n.code), ouvrir: onOuvrir }, draggable: !p.lecture, deletable: false, connectable: !p.lecture,
        ariaLabel: `${n.code} · ${n.libelle}`,
      })),
    ]
  }, [p.graphe.noeuds, p.positions, p.etapes, p.lecture, p.surlignes, onOuvrir, t])

  const arcs = useMemo<Edge[]>(
    () =>
      p.graphe.arcs.map((a) => ({
        id: `${a.de}>${a.vers}`, source: a.de, target: a.vers, type: 'smoothstep', deletable: !p.lecture, focusable: true,
        className: cx('gm-mod-arc', a.critique && 'gm-mod-arc-critique'), markerEnd: { type: MarkerType.ArrowClosed },
        ariaLabel: t('modelisateur.arc.aria', { de: a.de, vers: a.vers }),
      })),
    [p.graphe.arcs, p.lecture, t],
  )

  // Déposer une carte dans le couloir d'une autre étape la rattache à cette étape ; les couloirs sont ceux d'avant
  // le déplacement (positions encore non enregistrées), la position est enregistrée comme toute autre.
  const { onPlacer: placer } = p
  const onPlacer = useMemo(() => (l: { code: string; x: number; y: number }[]) => {
    placer(l)
    if (!onChangerEtape || p.lecture) return
    const lignes = calcules
      .filter((n): n is NoeudCouloir => n.type === 'couloir' && n.data.sens === 'etape')
      .map((n) => ({ cle: n.id, x: n.position.x, largeur: n.data.largeur }))
    for (const { code, x } of l) {
      const noeud = p.graphe.noeuds.find((n) => n.code === code)
      const cible = couloirSous(lignes, x)
      if (noeud && !noeud.fantome && cible !== undefined && cible !== (noeud.etape ?? null)) onChangerEtape(code, cible)
    }
  }, [placer, onChangerEtape, p.lecture, p.graphe.noeuds, calcules])

  // Canevas générique partagé avec les processus d'unité (D-87) : positions regroupées (PUT /positions), arcs.
  return (
    <CanevasGraphe<NoeudFlux> noeuds={calcules} arcs={arcs} types={TYPES as never} lecture={p.lecture} typesCartes={CARTES} prefixeAria="modelisateur.canevas"
      testId="modelisateur-canevas" onOuvrir={onOuvrir} onRelier={onRelier} onDelier={onDelier} onPlacer={onPlacer}
      classeMiniCarte={(n) => (n.type === 'couloir' ? 'gm-mod-mini-couloir' : 'gm-mod-mini-carte')} />
  )
}
