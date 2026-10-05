import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button, DataTable, Link, Note, PageHeader, Tabs, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import type { Rapport, VueRapports } from '../../../modele/pilotage'
import { useRapports } from '../../../hooks/pilotage'
import { PERM } from '../../../permissions'
import { useRetour } from '../parcours/communs'
import { etatListe } from '../liaisons/communs'
import { BadgeRapport, useDates } from './communs'
import { ConsultationRapport, DialogueRapport } from './dialoguesRapport'

// Rapports quotidiens et hebdomadaires (D-76, /des/rapports) : mes rapports, et ceux reçus de mon équipe
// (PIL_TEAM_READ) à viser ; création en fenêtre modale (préremplir, rédiger ou déposer un document),
// consultation et visa. ?vue=recus choisit l'onglet ; ?rapport=<id> ouvre un rapport ; ?auteur=<id> limite
// les rapports reçus à un collaborateur (depuis « Mon équipe »).

const PAR_PAGE = 20

export default function RapportsPage() {
  const { t } = useTranslation()
  const dates = useDates()
  const permissions = usePermissions()
  const recevables = aPermission(permissions, PERM.equipeLire)
  const [params, setParams] = useSearchParams()
  const vue: VueRapports = recevables && params.get('vue') === 'recus' ? 'recus' : 'moi'
  const auteur = vue === 'recus' ? params.get('auteur') : null
  const [page, setPage] = useState(1)
  const rapports = useRapports({ vue, page: page - 1, taille: PAR_PAGE })
  const [nouveau, setNouveau] = useState(false)
  const [retour, annoncer] = useRetour()
  const aller = (maj: Record<string, string | undefined>) => {
    const p = new URLSearchParams(params)
    Object.entries(maj).forEach(([k, v]) => (v ? p.set(k, v) : p.delete(k)))
    setParams(p)
  }
  const fait = (r: Rapport, message: string) => {
    setNouveau(false)
    aller({ rapport: r.id })
    annoncer(message)
  }
  const lignes = (rapports.data?.contenu ?? []).filter((r) => !auteur || r.auteur.id === auteur)
  const nomAuteur = auteur ? lignes[0]?.auteur.nomAffiche ?? auteur : null
  const colonnes: Column<Rapport>[] = [
    {
      key: 'periode', header: t('act.rapport.periode'), render: (r) => (
        <button type="button" className="gm-link" style={{ background: 'none', border: 0, padding: 0, font: 'inherit', textAlign: 'left', cursor: 'pointer' }} onClick={() => aller({ rapport: r.id })}>
          {dates.periode(r.type, r.periode)}
        </button>
      ),
    },
    { key: 'type', header: t('act.rapport.typeLibelle'), render: (r) => t(`act.rapport.type.${r.type}`) },
    ...(vue === 'recus' ? [{ key: 'auteur', header: t('act.rapport.auteur'), render: (r: Rapport) => r.auteur.nomAffiche }] : []),
    { key: 'mode', header: t('act.rapport.mode'), render: (r) => t(`act.rapport.modeValeur.${r.mode}`) },
    { key: 'etat', header: t('act.rapport.etat'), render: (r) => <BadgeRapport etat={r.etat} /> },
    { key: 'soumisLe', header: t('act.rapport.soumisLe'), render: (r) => (r.soumisLe ? dates.instant(r.soumisLe) : t('act.rapport.pasEncore')) },
  ]
  return (
    <div className="gm-col">
      <PageHeader
        title={t('act.rapports.titre')}
        subtitle={t('act.rapports.sousTitre')}
        actions={<Button variant="primary" icon="plus" onClick={() => setNouveau(true)}>{t('act.rapport.nouveau')}</Button>}
      />
      {recevables ? (
        <Tabs items={(['moi', 'recus'] as const).map((v) => ({ key: v, label: t(`act.rapports.vue.${v}`) }))} value={vue}
          onChange={(k) => { setPage(1); aller({ vue: k === 'moi' ? undefined : k, auteur: undefined }) }} />
      ) : null}
      {retour}
      {nomAuteur ? (
        <Note>
          {t('act.rapports.filtreAuteur', { nom: nomAuteur })} <Link href="/des/rapports?vue=recus">{t('act.rapports.tous')}</Link>
        </Note>
      ) : null}
      <div role="tabpanel" aria-label={t(`act.rapports.vue.${vue}`)}>
        <DataTable columns={colonnes} rows={lignes} rowKey={(r) => r.id} total={rapports.data?.total} page={page} pageSize={PAR_PAGE} onPage={setPage}
          emptyText={t(`act.rapports.vide.${vue}`)} {...etatListe(rapports, lignes)} onRetry={() => void rapports.refetch()} />
      </div>
      {nouveau ? <DialogueRapport onFermer={() => setNouveau(false)} onFait={fait} /> : null}
      {params.get('rapport') && !nouveau ? (
        <ConsultationRapport key={params.get('rapport')} id={params.get('rapport')!} onFermer={() => aller({ rapport: undefined })} onFait={(_r, message) => { aller({ rapport: undefined }); annoncer(message) }} />
      ) : null}
    </div>
  )
}
