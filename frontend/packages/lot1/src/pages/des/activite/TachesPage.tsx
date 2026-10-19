import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button, DataTable, KanbanBoard, PageHeader, Select, Tabs, type Column, type KanbanColumn } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { ETATS_TACHE, TRANSITIONS_TACHE, type EtatTache, type Tache, type VueTaches } from '../../../modele/pilotage'
import { useTaches } from '../../../hooks/pilotage'
import { usePouvoirs } from '../../../hooks/pouvoirs'
import { PERM } from '../../../permissions'
import { useRetour } from '../parcours/communs'
import { etatListe } from '../liaisons/communs'
import { BadgePriorite, BadgeTache, ETAT_TACHE, useDates, useLibelleUnite } from './communs'
import { DetailTache, DialogueAttribution, DialogueGroupe, DialogueTache, DialogueTransition, type Destination } from './dialoguesTache'

// Tâches (D-76, /des/taches ; CP PIL-04, PIL-08 ; D-81 §6) : mes tâches, les tâches à prendre de mes unités,
// celles que j'ai confiées (PIL_TASK_ASSIGN) et celles de mon équipe (PIL_TEAM_READ), filtrées par état et
// paginées par le service. ?vue= choisit l'onglet, ?tache=<id> ouvre la fiche, ?groupe=<id> le suivi d'un groupe ;
// ?confier=<compte> ou ?unite=<code> ouvre la création pré-remplie (depuis « Mon équipe ») ; création,
// modification, transitions et attribution dans des fenêtres modales. Personnes et unités proposées : la portée
// TACHE_CONFIER de la matrice des pouvoirs (GET pouvoirs/moi).

const PAR_PAGE = 20
/** Tableau Kanban (D-113) : toutes les tâches de la vue, sans les annulées, une colonne par état. */
const PAR_TABLEAU = 200
const COLONNES_TABLEAU: EtatTache[] = ['A_PRENDRE', 'OUVERTE', 'EN_COURS', 'BLOQUEE', 'TERMINEE']

type Dialogue = { nouvelle: true } | { modifier: Tache } | { transition: Tache; vers: EtatTache } | { attribuer: Tache } | null

export default function TachesPage() {
  const { t } = useTranslation()
  const dates = useDates()
  const permissions = usePermissions()
  const peutConfier = aPermission(permissions, PERM.tacheConfier)
  const equipeLisible = aPermission(permissions, PERM.equipeLire)
  const [params, setParams] = useSearchParams()
  const vues: VueTaches[] = ['moi', 'unite', ...(peutConfier ? ['creees' as const] : []), ...(equipeLisible ? ['equipe' as const] : [])]
  const vue = vues.find((v) => v === params.get('vue')) ?? 'moi'
  const [etat, setEtat] = useState<EtatTache | ''>('')
  const [page, setPage] = useState(1)
  // Affichage en liste (paginée par le service) ou en tableau Kanban pour organiser son travail (D-113, ?affichage=tableau).
  const tableau = params.get('affichage') === 'tableau'
  const taches = useTaches(tableau ? { vue, page: 0, taille: PAR_TABLEAU } : { vue, etat: etat || undefined, page: page - 1, taille: PAR_PAGE })
  const pouvoirs = usePouvoirs(peutConfier || equipeLisible)
  const libelleUnite = useLibelleUnite()
  const confier = params.get('confier')
  const unite = params.get('unite')
  const [dialogue, setDialogue] = useState<Dialogue>(() => (confier || unite ? { nouvelle: true } : null))
  const initiale: { destination?: Destination; responsableId?: string; uniteCible?: string } | undefined = unite ? { destination: 'unite', uniteCible: unite } : confier ? { responsableId: confier } : undefined
  const [retour, annoncer] = useRetour()
  const aller = (maj: Record<string, string | undefined>) => {
    const p = new URLSearchParams(params)
    Object.entries(maj).forEach(([k, v]) => (v ? p.set(k, v) : p.delete(k)))
    setParams(p)
  }
  const fait = (message: string) => {
    setDialogue(null)
    if (confier || unite) aller({ confier: undefined, unite: undefined })
    annoncer(message)
  }
  const fermer = () => {
    setDialogue(null)
    if (confier || unite) aller({ confier: undefined, unite: undefined })
  }
  const ouverte = params.get('tache')
  const lignes = taches.data?.contenu ?? []
  const groupe = params.get('groupe')
  const colonnes: Column<Tache>[] = [
    {
      key: 'titre', header: t('act.tache.titre'), render: (x) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <button type="button" className="gm-link" style={{ background: 'none', border: 0, padding: 0, font: 'inherit', textAlign: 'left', cursor: 'pointer' }} onClick={() => aller({ tache: x.id })}>{x.titre}</button>
          {x.origine.type !== 'INTERNE' ? <span className="gm-field-help">{t(`act.tache.origine.${x.origine.type}`)}{x.origine.reference ? ` · ${x.origine.reference}` : ''}</span> : null}
          {x.profilRequis ? <span className="gm-field-help">{t('proc.tache.profilRequisDe', { profil: t(`compte.profil.${x.profilRequis}`, { defaultValue: x.profilRequis }) })}</span> : null}
          {/* Tâche offerte par permission ou par profils (§8.8) : libellés lisibles des permissions et profils requis. */}
          {x.permissionsRequises?.length ? <span className="gm-field-help">{t('pvr.tache.permissionsRequises', { liste: x.permissionsRequises.map((p) => t(`droits.permission.${p}`, { defaultValue: p })).join(', ') })}</span> : null}
          {x.profilsRequis?.length ? <span className="gm-field-help">{t('pvr.tache.profilsRequis', { liste: x.profilsRequis.map((p) => t(`compte.profil.${p}`, { defaultValue: p })).join(', ') })}</span> : null}
        </span>
      ),
    },
    { key: 'priorite', header: t('act.tache.priorite'), render: (x) => <BadgePriorite priorite={x.priorite} /> },
    { key: 'echeance', header: t('act.tache.echeance'), render: (x) => (x.echeance ? dates.jour(x.echeance) : t('act.tache.sansEcheance')) },
    { key: 'etat', header: t('act.tache.etat'), render: (x) => <BadgeTache etat={x.etat} enRetard={x.enRetard} /> },
    vue === 'moi' || vue === 'unite'
      ? { key: 'createur', header: t('act.tache.createur'), render: (x) => x.createur.nomAffiche }
      : {
        key: 'responsable', header: t('act.tache.responsable'), render: (x) => (
          <span className="gm-col" style={{ gap: 0 }}>
            <span>{x.responsable?.nomAffiche ?? (x.uniteCible ? t('pvr.tache.aPrendreUnite', { unite: libelleUnite(x.uniteCible) }) : t('pvr.tache.aPrendreSansUnite'))}</span>
            {x.groupeId && vue === 'creees' ? (
              <button type="button" className="gm-link" style={{ background: 'none', border: 0, padding: 0, font: 'inherit', textAlign: 'left', cursor: 'pointer' }}
                aria-label={t('pvr.groupe.suiviDe', { titre: x.titre })} onClick={() => aller({ groupe: x.groupeId! })}>{t('pvr.groupe.suivi')}</button>
            ) : null}
          </span>
        ),
      },
    ...(vue === 'unite' ? [{ key: 'unite', header: t('pvr.tache.unite'), render: (x: Tache) => (x.uniteCible ? libelleUnite(x.uniteCible) : t('pvr.tache.toutesUnites')) }] : []),
  ]
  return (
    <div className="gm-col">
      <PageHeader
        title={t('act.taches.titre')}
        subtitle={t('act.taches.sousTitre')}
        actions={<Button variant="primary" icon="plus" onClick={() => setDialogue({ nouvelle: true })}>{t('act.tache.nouvelle')}</Button>}
      />
      {vues.length > 1 ? (
        <Tabs items={vues.map((v) => ({ key: v, label: t(`act.taches.vue.${v}`) }))} value={vue} onChange={(k) => { setPage(1); aller({ vue: k === 'moi' ? undefined : k }) }} />
      ) : null}
      {retour}
      <div className="gm-row" style={{ justifyContent: 'flex-end' }}>
        <Tabs items={[{ key: 'liste', label: t('act.taches.affichage.liste') }, { key: 'tableau', label: t('act.taches.affichage.tableau') }]} value={tableau ? 'tableau' : 'liste'}
          onChange={(k) => aller({ affichage: k === 'tableau' ? 'tableau' : undefined })} />
      </div>
      <div role="tabpanel" aria-label={t(`act.taches.vue.${vue}`)}>
        {tableau ? (
          <KanbanBoard
            label={t('act.taches.affichage.tableau')}
            masquerAjout
            columns={COLONNES_TABLEAU.filter((e) => e !== 'A_PRENDRE' || vue === 'unite' || lignes.some((x) => x.etat === 'A_PRENDRE')).map((e): KanbanColumn => ({
              key: e,
              label: t(`act.tache.etat.${e}`),
              onAdd: e === 'OUVERTE' ? () => setDialogue({ nouvelle: true }) : undefined,
              cards: lignes.filter((x) => x.etat === e).map((x) => ({
                id: x.id,
                reference: x.origine.reference ?? t(`act.tache.origine.${x.origine.type}`),
                title: x.titre,
                institution: vue === 'moi' || vue === 'unite' ? x.createur.nomAffiche : x.responsable?.nomAffiche ?? (x.uniteCible ? libelleUnite(x.uniteCible) : ''),
                type: t(`act.priorite.${x.priorite}`),
                status: x.enRetard ? 'crit' : ETAT_TACHE[x.etat],
                statusLabel: x.enRetard ? t('act.tache.enRetard') : t(`act.tache.etat.${x.etat}`),
                due: x.echeance ? dates.jour(x.echeance) : t('act.tache.sansEcheance'),
                late: x.enRetard,
              })),
            }))}
            canMove={(id, vers) => {
              const x = lignes.find((l) => l.id === id)
              return !!x && TRANSITIONS_TACHE[x.etat].includes(vers as EtatTache)
            }}
            onMove={(id, vers) => {
              const x = lignes.find((l) => l.id === id)
              if (x) setDialogue({ transition: x, vers: vers as EtatTache })
            }}
            onOpen={(id) => aller({ tache: id })}
          />
        ) : (
        <DataTable
          columns={colonnes}
          rows={lignes}
          rowKey={(x) => x.id}
          total={taches.data?.total}
          page={page}
          pageSize={PAR_PAGE}
          onPage={setPage}
          emptyText={t(`act.taches.vide.${vue}`)}
          toolbar={(
            <div style={{ minWidth: 220 }}>
              <Select name="etat" label={t('act.tache.filtreEtat')} value={etat} onChange={(v) => { setEtat(v as EtatTache | ''); setPage(1) }}
                options={[{ value: '', label: t('act.tache.tousEtats') }, ...ETATS_TACHE.map((e) => ({ value: e, label: t(`act.tache.etat.${e}`) }))]} />
            </div>
          )}
          {...etatListe(taches, lignes)}
          onRetry={() => void taches.refetch()}
        />
        )}
      </div>
      {ouverte && !dialogue && !groupe ? (
        <DetailTache id={ouverte} pouvoirs={pouvoirs.data} onFermer={() => aller({ tache: undefined })} onModifier={(x) => setDialogue({ modifier: x })}
          onTransition={(x, vers) => setDialogue({ transition: x, vers })} onAttribuer={(x) => setDialogue({ attribuer: x })} onGroupe={(g) => aller({ groupe: g })} onFait={annoncer} />
      ) : null}
      {groupe && !dialogue ? <DialogueGroupe groupeId={groupe} onFermer={() => aller({ groupe: undefined })} /> : null}
      {dialogue && 'nouvelle' in dialogue && (!peutConfier || !pouvoirs.isPending) ? (
        <DialogueTache pouvoirs={pouvoirs.data} peutConfier={peutConfier} initiale={initiale} onFermer={fermer} onFait={fait} />
      ) : null}
      {dialogue && 'modifier' in dialogue ? <DialogueTache tache={dialogue.modifier} pouvoirs={pouvoirs.data} peutConfier={peutConfier} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
      {dialogue && 'transition' in dialogue ? <DialogueTransition tache={dialogue.transition} vers={dialogue.vers} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
      {dialogue && 'attribuer' in dialogue ? <DialogueAttribution tache={dialogue.attribuer} pouvoirs={pouvoirs.data} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
    </div>
  )
}
