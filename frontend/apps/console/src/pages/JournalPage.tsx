import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { CorrelationIdChip, DataTable, Link, Select, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { dateHeure } from '@gimac/compte'
import { useComptes, useJournal } from '../hooks'
import type { EntreeJournal } from '../modele/comptes'
import { etatListe } from '../composants/formats'

// Journal d'administration (D-62, IAM_JOURNAL_READ) : chaque acte de la console (création, profils,
// sécurité, configuration), avec son auteur, son motif et son identifiant de corrélation ; paginé,
// filtrable par compte (?compte=<id>).
export default function JournalPage() {
  const { t, i18n } = useTranslation()
  const permissions = usePermissions()
  const [params, setParams] = useSearchParams()
  const compte = params.get('compte') ?? ''
  const [page, setPage] = useState(1)
  const [taille, setTaille] = useState(25)
  const journal = useJournal({ page, taille, compte: compte || undefined })
  const comptes = useComptes({ page: 1, taille: 100 }, aPermission(permissions, 'IAM_ACCOUNT_READ'))
  const lignes = journal.data?.contenu ?? []

  const colonnes: Column<EntreeJournal>[] = [
    { key: 'horodatage', header: t('console.journal.horodatage'), render: (j) => dateHeure(j.horodatage, i18n.language) },
    { key: 'acteurNom', header: t('console.journal.acteur'), render: (j) => j.acteurNom ?? j.acteurId },
    { key: 'action', header: t('console.journal.action'), render: (j) => t(`console.journal.action.${j.action}`, { defaultValue: j.action }) },
    {
      key: 'cibleLibelle', header: t('console.journal.cible'),
      render: (j) => (j.cibleId ? <Link href={`/console/comptes/${j.cibleId}`} mono>{j.cibleLibelle ?? j.cibleId}</Link> : <span className="gm-mono">{j.cibleLibelle ?? '—'}</span>),
    },
    { key: 'motif', header: t('console.journal.motif'), render: (j) => j.motif ?? '—' },
    { key: 'details', header: t('console.journal.details'), render: (j) => j.details || '—' },
    { key: 'correlationId', header: t('console.journal.correlation'), render: (j) => (j.correlationId ? <CorrelationIdChip id={j.correlationId} /> : '—') },
  ]

  return (
    <div className="gm-col">
      <h1 className="gm-heading-display">{t('console.journal.titre')}</h1>
      <div style={{ maxWidth: 420 }}>
        <Select
          name="compte"
          label={t('console.journal.filtreCompte')}
          value={compte}
          onChange={(v) => {
            setPage(1)
            setParams(v ? { compte: v } : {})
          }}
          options={[{ value: '', label: t('console.journal.tousComptes') }, ...(comptes.data?.contenu ?? []).map((c) => ({ value: c.id, label: `${c.nomAffiche} (${c.identifiant})` }))]}
        />
      </div>
      <DataTable
        columns={colonnes}
        rows={lignes}
        rowKey={(j) => j.id}
        total={journal.data?.total}
        page={page}
        pageSize={taille}
        onPage={setPage}
        onPageSize={(n) => {
          setTaille(n)
          setPage(1)
        }}
        onRetry={() => void journal.refetch()}
        emptyText={t('console.journal.vide')}
        density="compact"
        {...etatListe(journal, lignes)}
      />
    </div>
  )
}
