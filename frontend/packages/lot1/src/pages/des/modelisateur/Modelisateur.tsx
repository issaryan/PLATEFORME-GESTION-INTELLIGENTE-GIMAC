import { useCallback, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { ReactFlowProvider, useReactFlow } from '@xyflow/react'
import { Button, Icon, InlineAlert, Select, cx } from '@gimac/design-system'
import { EtatsRequete, aPermission, usePermissions, type ApiError } from '@gimac/socle'
import {
  useArcReferentiel, useCatalogueReferentiels, useCreerOperation, useDesignerPilote, useEnregistrerOperation, useGrapheReferentiel, usePlacerOperations, useSupprimerOperation,
} from '../../../hooks/referentiels'
import { useExecuteurActions } from '../../../ressources/actions'
import { actionPublier } from '../../../ressources/referentielPublication'
import type { CatalogueModelisateur, GrapheReferentiel, ReferenceOperation, ReferentielVersion } from '../../../modele/homologation'
import { CARTE, dispositionAuto, operationVierge, positionsEffectives, saisieDe, sourceEtapes } from '../../../adaptateurs/modelisateur'
import { BandeauConcerne, BoutonExport } from '../compositions/referentiel'
import { ModaleEtapes } from './ModaleEtapes'
import { Canevas } from './Canevas'
import { ModaleOperation } from './ModaleOperation'
import { VueListe } from './VueListe'
import { DialogueFonction } from '../referentiels/TypesProjetPage'

// Onglet « Modélisateur » de DES-12 (D-77, §6) : canevas ou vue liste équivalente, barre d'outils (ajouter,
// disposition automatique, contrôler, comparer, publier, pilote du brouillon), modale de l'opération. Brouillon :
// édition ; version publiée ou retirée : lecture seule (le service le dit par actionsPermises). D-89 : tout éditeur
// (HOM_REFERENTIEL_EDIT) modifie un brouillon, le pilote n'est que le responsable affiché ; couloirs = étapes de la
// version (ordre, couleur), modale « Étapes », lecture filtrée (fantômes grisés, bandeau), export CSV.

type Modale = { creation: true } | { creation: false; code: string }

export function OngletModelisateur({ version }: { version: ReferentielVersion }) {
  return (
    <ReactFlowProvider>
      <Modelisateur version={version} />
    </ReactFlowProvider>
  )
}

function Modelisateur({ version }: { version: ReferentielVersion }) {
  const { t } = useTranslation()
  const graphe = useGrapheReferentiel(version.id)
  // Catalogue de la version (D-89) : ses étapes servent au sélecteur de la modale d'opération.
  const catalogue = useCatalogueReferentiels(version.id)
  const etats = { emptyTitle: t('commun.vide'), errorTitle: t('commun.erreur.titre'), retryLabel: t('commun.reessayer') }
  return (
    <EtatsRequete etat={catalogue} {...etats}>
      {(c) => (
        <EtatsRequete etat={graphe} {...etats}>
          {(g) => <Espace version={version} graphe={g} catalogue={c} />}
        </EtatsRequete>
      )}
    </EtatsRequete>
  )
}

function Espace({ version, graphe, catalogue }: { version: ReferentielVersion; graphe: GrapheReferentiel; catalogue: CatalogueModelisateur }) {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const vue = params.get('vue') === 'liste' ? 'liste' : 'canevas'
  const permissions = usePermissions()
  const executeur = useExecuteurActions()
  const flux = useReactFlow()
  const lecture = !graphe.actionsPermises.includes('MODIFIER') || !aPermission(permissions, 'HOM_REFERENTIEL_EDIT')
  const peutPublier = graphe.actionsPermises.includes('PUBLIER') && aPermission(permissions, actionPublier.permission ?? '')
  const [modale, setModale] = useState<Modale | null>(null)
  const [etapesOuvertes, setEtapesOuvertes] = useState(false)
  const [nouvelleFonction, setNouvelleFonction] = useState(false)
  const [controle, setControle] = useState(false)
  const [focus, setFocus] = useState<string | null>(null)
  const [erreur, setErreur] = useState<ApiError | null>(null)
  const [annonce, setAnnonce] = useState('')
  const creer = useCreerOperation(version.id)
  const enregistrer = useEnregistrerOperation(version.id)
  const supprimer = useSupprimerOperation(version.id)
  const arc = useArcReferentiel(version.id)
  const placer = usePlacerOperations(version.id)
  const pilote = useDesignerPilote(version.id)

  // Lecture concernée (D-89) : seuls les couloirs des étapes où figure une opération visible, fantômes à la suite.
  const source = useMemo(() => {
    const s = sourceEtapes(graphe.etapes, catalogue)
    return graphe.filtre === 'CONCERNE' && s.version ? { ...s, etapes: s.etapes.filter((e) => graphe.noeuds.some((n) => n.etape === e.code)) } : s
  }, [graphe.etapes, graphe.filtre, graphe.noeuds, catalogue])
  const auto = useMemo(() => dispositionAuto(graphe.noeuds, source), [graphe.noeuds, source])
  // Lecture concernée (D-89) : positions enregistrées pour le graphe complet ignorées, disposition automatique des
  // opérations visibles et de leurs fantômes (couloirs alignés, cadrage serré).
  const positions = useMemo(() => (graphe.filtre === 'CONCERNE' ? auto : positionsEffectives(graphe.noeuds, auto)), [graphe.filtre, graphe.noeuds, auto])
  const surlignes = useMemo(() => {
    const s = new Set<string>(controle ? graphe.anomalies.flatMap((a) => a.codes) : [])
    if (focus) s.add(focus)
    return s
  }, [controle, graphe.anomalies, focus])

  const echec = useCallback((e: ApiError) => setErreur(e), [])
  // Un fantôme (D-89) ne s'ouvre pas : il n'en est servi que le code et le libellé.
  const ouvrir = useCallback((code: string) => {
    if (!graphe.noeuds.find((n) => n.code === code)?.fantome) setModale({ creation: false, code })
  }, [graphe.noeuds])
  const relier = useCallback((de: string, vers: string) => {
    setErreur(null)
    arc.mutate({ de, vers, present: true }, { onError: echec, onSuccess: () => setAnnonce(t('modelisateur.annonce.relie', { de, vers })) })
  }, [arc, echec, t])
  const delier = useCallback((de: string, vers: string) => {
    setErreur(null)
    arc.mutate({ de, vers, present: false }, { onError: echec, onSuccess: () => setAnnonce(t('modelisateur.annonce.delie', { de, vers })) })
  }, [arc, echec, t])
  // Carte déposée dans un autre couloir : l'opération change d'étape (saisie complète, comme la modale).
  const changerEtape = useCallback((code: string, etape: string | null) => {
    const o = graphe.noeuds.find((n) => n.code === code)
    if (!o) return
    setErreur(null)
    enregistrer.mutate({ code, operation: { ...saisieDe(o), etape: etape as ReferenceOperation['etape'] } }, {
      onError: echec,
      onSuccess: () => setAnnonce(t('modelisateur.annonce.etapeChangee', {
        code, etape: etape ? graphe.etapes?.find((e) => e.code === etape)?.libelle ?? etape : t('modelisateur.sansEtape'),
      })),
    })
  }, [graphe.noeuds, graphe.etapes, enregistrer, echec, t])
  const placerPositions = useCallback((l: { code: string; x: number; y: number }[]) => placer.mutate(l, { onError: echec }), [placer, echec])
  const disposer = () => {
    setErreur(null)
    placer.mutate([...auto.entries()].map(([code, p]) => ({ code, x: p.x, y: p.y })), {
      onError: echec,
      onSuccess: () => {
        setAnnonce(t('modelisateur.annonce.dispose'))
        window.setTimeout(() => void flux.fitView({ padding: 0.1, duration: 300 }), 50)
      },
    })
  }
  const centrer = (code: string) => {
    setFocus(code)
    if (vue === 'canevas') {
      const p = positions.get(code)
      if (p) void flux.setCenter(p.x + CARTE.largeur / 2, p.y + CARTE.hauteur / 2, { zoom: 1.1, duration: 300 })
    } else {
      document.querySelector<HTMLButtonElement>(`[data-testid="modelisateur-liste"] button[aria-label="${t('modelisateur.action.modifierOp', { code })}"]`)?.focus()
    }
  }
  const basculerVue = (v: 'canevas' | 'liste') => setParams((x) => {
    const n = new URLSearchParams(x)
    if (v === 'liste') n.set('vue', 'liste')
    else n.delete('vue')
    return n
  })
  const comparer = () => setParams((x) => ({ ...Object.fromEntries(x), onglet: 'comparaison' }))

  const operation: ReferenceOperation | null = modale ? (modale.creation ? operationVierge() : graphe.noeuds.find((n) => n.code === modale.code) ?? null) : null
  const mutationModale = modale?.creation ? creer : enregistrer
  const fermerModale = () => {
    setModale(null)
    creer.reset()
    enregistrer.reset()
    supprimer.reset()
  }
  const pilotes = catalogue.pilotes
  const nomPilote = pilotes.find((x) => x.id === graphe.piloteId)?.nom

  return (
    <div className="gm-col" data-testid="modelisateur" style={{ gap: 'var(--gm-space-3)' }}>
      <div role="toolbar" aria-label={t('modelisateur.outils')} className="gm-mod-outils">
        <div className="gm-row" role="group" aria-label={t('modelisateur.vue.titre')} style={{ gap: 0 }}>
          {(['canevas', 'liste'] as const).map((v) => (
            <button key={v} type="button" className={cx('gm-btn', 'gm-btn-sm', vue === v ? 'gm-btn-primary' : 'gm-btn-secondary')} aria-pressed={vue === v} onClick={() => basculerVue(v)}>
              <Icon name={v === 'canevas' ? 'kanban' : 'menu'} size={20} />
              {t(`modelisateur.vue.${v}`)}
            </button>
          ))}
        </div>
        {!lecture ? <Button size="sm" icon="plus" onClick={() => setModale({ creation: true })}>{t('modelisateur.action.ajouter')}</Button> : null}
        {!lecture ? <Button size="sm" icon="refresh" loading={placer.isPending} onClick={disposer}>{t('modelisateur.action.disposer')}</Button> : null}
        <Button size="sm" icon="shield-check" aria-expanded={controle} onClick={() => setControle((x) => !x)}>{controle ? t('modelisateur.action.masquerControle') : t('modelisateur.action.controler')}</Button>
        <Button size="sm" icon="copy" onClick={comparer}>{t('modelisateur.action.comparer')}</Button>
        {graphe.etapes?.length ? <Button size="sm" icon="kanban" onClick={() => setEtapesOuvertes(true)}>{t('modelisateur.action.etapes')}</Button> : null}
        <BoutonExport versionId={version.id} />
        {peutPublier ? <Button size="sm" variant="primary" icon="check" onClick={() => executeur.lancer(actionPublier, version, version.libelle, 'homologation')}>{t(actionPublier.label)}</Button> : null}
        {!lecture && version.etat === 'BROUILLON' ? (
          <Select name="pilote" label={t('modelisateur.pilote')} help={t('modelisateur.aide.pilote')} value={graphe.piloteId ?? ''}
            options={[{ value: '', label: t('modelisateur.aucun') }, ...pilotes.map((x) => ({ value: x.id, label: x.nom }))]}
            onChange={(v) => { if (v) pilote.mutate(v, { onError: echec, onSuccess: () => setAnnonce(t('modelisateur.annonce.pilote')) }) }} />
        ) : null}
      </div>
      <div className="gm-mod-resume">
        <span>{t('modelisateur.resume.operations', { count: graphe.noeuds.filter((n) => !n.fantome).length })}</span>
        <span>{t('modelisateur.resume.critique', { n: graphe.cheminCritique.dureeJours, count: graphe.cheminCritique.codes.length })}</span>
        <span>{t('modelisateur.resume.anomalies', { count: graphe.anomalies.length })}</span>
        {version.etat === 'BROUILLON' ? <span>{t('modelisateur.resume.pilote', { nom: nomPilote ?? t('modelisateur.aucun') })}</span> : null}
        {lecture ? <span>{t(graphe.actionsPermises.includes('MODIFIER') ? 'modelisateur.lectureDroits' : 'modelisateur.lectureSeule')}</span> : null}
      </div>
      {graphe.filtre === 'CONCERNE' ? <BandeauConcerne /> : null}
      {erreur ? (
        <InlineAlert level="crit" title={t(`erreur.${erreur.code}`, { defaultValue: erreur.message })} correlationId={erreur.correlationId || undefined}
          action={<Button size="sm" variant="ghost" icon="x" onClick={() => setErreur(null)}>{t('commun.fermer')}</Button>} />
      ) : null}
      <div className="gm-mod-sr" role="status" aria-live="polite">{annonce}</div>
      {controle ? (
        <section aria-label={t('modelisateur.controle.titre')} className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
          <h3 className="gm-type-h3" style={{ margin: 0 }}>{t('modelisateur.controle.titre')}</h3>
          {graphe.anomalies.length || graphe.manquants.length ? (
            <ul className="gm-mod-anomalies-liste">
              {graphe.anomalies.map((a, i) => (
                <li key={`${a.type}-${i}`}>
                  <button type="button" className="gm-mod-anomalie-lien" onClick={() => a.codes[0] && centrer(a.codes[0])}>
                    {/* HOM-02 : les codes d'un geste de certification absent sont un jalon et des sujets d'événement, dits par le motif. */}
                    <strong>{t(`modelisateur.anomalie.${a.type}`)}</strong>{a.codes.length && a.type !== 'GESTE_CERTIFICATION_ABSENT' ? ` · ${a.codes.join(', ')}` : ''}{a.motif ? ` · ${a.motif}` : ''}
                  </button>
                </li>
              ))}
              {graphe.manquants.map((m) => (
                <li key={`m-${m.code}`}>
                  <button type="button" className="gm-mod-anomalie-lien" onClick={() => centrer(m.code)}>
                    <strong>{t('modelisateur.controle.manquant')}</strong>{` · ${m.code} · ${m.attributs.map((x) => t(`modelisateur.attribut.${x}`, { defaultValue: x })).join(', ')}`}
                  </button>
                </li>
              ))}
            </ul>
          ) : <InlineAlert level="info">{t('modelisateur.controle.conforme')}</InlineAlert>}
        </section>
      ) : null}
      {vue === 'canevas' ? (
        <Canevas graphe={graphe} catalogue={catalogue} etapes={source} positions={positions} lecture={lecture} surlignes={surlignes} onOuvrir={ouvrir} onRelier={relier} onDelier={delier} onPlacer={placerPositions} onChangerEtape={changerEtape} />
      ) : (
        <VueListe graphe={graphe} etapes={source} lecture={lecture} surlignes={surlignes} onOuvrir={ouvrir} onRelier={relier} onDelier={delier} />
      )}
      {etapesOuvertes && graphe.etapes ? (
        <ModaleEtapes versionId={version.id} etapes={graphe.etapes} catalogue={catalogue} lecture={lecture} onFermer={() => setEtapesOuvertes(false)} onAnnonce={setAnnonce} />
      ) : null}
      {modale && operation ? (
        <ModaleOperation
          key={modale.creation ? 'creation' : modale.code}
          operation={operation}
          creation={modale.creation}
          lectureSeule={lecture}
          catalogue={catalogue}
          noeuds={graphe.noeuds}
          enCours={mutationModale.isPending || supprimer.isPending}
          erreurServeur={mutationModale.error ?? supprimer.error ?? null}
          onFermer={fermerModale}
          onEnregistrer={(o) => {
            if (modale.creation) creer.mutate({ ...saisieDe(o), code: o.code }, { onSuccess: () => { fermerModale(); setAnnonce(t('modelisateur.annonce.creee', { code: o.code })) } })
            else enregistrer.mutate({ code: o.code, operation: saisieDe(o) }, { onSuccess: () => { fermerModale(); setAnnonce(t('modelisateur.annonce.enregistree', { code: o.code })) } })
          }}
          onSupprimer={() => supprimer.mutate(operation.code, { onSuccess: () => { fermerModale(); setAnnonce(t('modelisateur.annonce.supprimee', { code: operation.code })) } })}
          onNouvelleFonction={() => setNouvelleFonction(true)}
        />
      ) : null}
      {/* « Nouvelle fonction » (D-91) : créée au catalogue, aussitôt proposée dans les sélecteurs (catalogue relu). */}
      {nouvelleFonction ? <DialogueFonction onFermer={() => setNouvelleFonction(false)} onFait={(f) => { setNouvelleFonction(false); setAnnonce(t('typesProjet.fonction.fait', { libelle: f.libelle })) }} /> : null}
    </div>
  )
}
