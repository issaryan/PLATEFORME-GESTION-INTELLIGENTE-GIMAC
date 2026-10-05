import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button, DataTable, PageHeader, Select, Tabs, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { ETATS_TACHE, type EtatTache, type Tache, type VueTaches } from '../../../modele/pilotage'
import { aujourdhui } from '../../../modele/jours'
import { useEquipe, useTaches } from '../../../hooks/pilotage'
import { PERM } from '../../../permissions'
import { useRetour } from '../parcours/communs'
import { etatListe } from '../liaisons/communs'
import { BadgePriorite, BadgeTache, useDates } from './communs'
import { DetailTache, DialogueTache, DialogueTransition } from './dialoguesTache'

// Tâches (D-76, /des/taches ; CP PIL-04, PIL-08) : mes tâches, celles que j'ai confiées (PIL_TASK_ASSIGN) et
// celles de mon équipe (PIL_TEAM_READ), filtrées par état et paginées par le service. ?vue= choisit l'onglet,
// ?tache=<id> ouvre la fiche ; création, modification et transitions dans des fenêtres modales.

const PAR_PAGE = 20

type Dialogue = { nouvelle: true } | { modifier: Tache } | { transition: Tache; vers: EtatTache } | null

export default function TachesPage() {
  const { t } = useTranslation()
  const dates = useDates()
  const permissions = usePermissions()
  const peutConfier = aPermission(permissions, PERM.tacheConfier)
  const equipeLisible = aPermission(permissions, PERM.equipeLire)
  const [params, setParams] = useSearchParams()
  const vues: VueTaches[] = ['moi', ...(peutConfier ? ['creees' as const] : []), ...(equipeLisible ? ['equipe' as const] : [])]
  const vue = vues.find((v) => v === params.get('vue')) ?? 'moi'
  const [etat, setEtat] = useState<EtatTache | ''>('')
  const [page, setPage] = useState(1)
  const taches = useTaches({ vue, etat: etat || undefined, page: page - 1, taille: PAR_PAGE })
  const equipe = useEquipe(aujourdhui(), peutConfier)
  const [dialogue, setDialogue] = useState<Dialogue>(null)
  const [retour, annoncer] = useRetour()
  const aller = (maj: Record<string, string | undefined>) => {
    const p = new URLSearchParams(params)
    Object.entries(maj).forEach(([k, v]) => (v ? p.set(k, v) : p.delete(k)))
    setParams(p)
  }
  const fait = (message: string) => {
    setDialogue(null)
    annoncer(message)
  }
  const ouverte = params.get('tache')
  const lignes = taches.data?.contenu ?? []
  const membres = (equipe.data ?? []).map((m) => ({ id: m.collaborateur.id, nomAffiche: m.collaborateur.nomAffiche }))
  const colonnes: Column<Tache>[] = [
    {
      key: 'titre', header: t('act.tache.titre'), render: (x) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <button type="button" className="gm-link" style={{ background: 'none', border: 0, padding: 0, font: 'inherit', textAlign: 'left', cursor: 'pointer' }} onClick={() => aller({ tache: x.id })}>{x.titre}</button>
          {x.origine.type !== 'INTERNE' ? <span className="gm-field-help">{t(`act.tache.origine.${x.origine.type}`)}{x.origine.reference ? ` · ${x.origine.reference}` : ''}</span> : null}
        </span>
      ),
    },
    { key: 'priorite', header: t('act.tache.priorite'), render: (x) => <BadgePriorite priorite={x.priorite} /> },
    { key: 'echeance', header: t('act.tache.echeance'), render: (x) => (x.echeance ? dates.jour(x.echeance) : t('act.tache.sansEcheance')) },
    { key: 'etat', header: t('act.tache.etat'), render: (x) => <BadgeTache etat={x.etat} enRetard={x.enRetard} /> },
    vue === 'moi'
      ? { key: 'createur', header: t('act.tache.createur'), render: (x) => x.createur.nomAffiche }
      : { key: 'responsable', header: t('act.tache.responsable'), render: (x) => x.responsable.nomAffiche },
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
      <div role="tabpanel" aria-label={t(`act.taches.vue.${vue}`)}>
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
      </div>
      {ouverte && !dialogue ? (
        <DetailTache id={ouverte} onFermer={() => aller({ tache: undefined })} onModifier={(x) => setDialogue({ modifier: x })} onTransition={(x, vers) => setDialogue({ transition: x, vers })} />
      ) : null}
      {dialogue && 'nouvelle' in dialogue ? <DialogueTache membres={membres} peutConfier={peutConfier} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
      {dialogue && 'modifier' in dialogue ? <DialogueTache tache={dialogue.modifier} membres={membres} peutConfier={peutConfier} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
      {dialogue && 'transition' in dialogue ? <DialogueTransition tache={dialogue.transition} vers={dialogue.vers} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
    </div>
  )
}
