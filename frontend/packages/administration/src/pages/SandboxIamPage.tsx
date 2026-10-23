import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Banner, DataTable, PageHeader, StatusBadge, Tabs, type Column } from '@gimac/design-system'
import { dateHeure } from '@gimac/compte'
import { useComptesProteges, usePerimetresSuspendus } from '../hooks/sandbox'
import type { CompteProtege, PerimetreSuspendu } from '../modele/sandbox'
import { etatListe } from '../composants/formats'

// Sandbox GIMAC dans la console (D-119, docs/lot1/sandbox-existant-adapte.md §6), IAM_ACCOUNT_READ : ce que MS-IAM
// applique pour la sandbox, en lecture. Onglet « Périmètres » : institutions dont l'espace est suspendu (introspection
// refusée, sessions révoquées, état des comptes inchangé) ; « Comptes protégés » : derniers administrateurs actifs,
// épargnés par la désactivation pour inactivité. Onglet tenu dans l'adresse (?onglet=).
export const ONGLETS_SANDBOX = ['perimetres', 'protections'] as const
export type OngletSandbox = (typeof ONGLETS_SANDBOX)[number]

function Perimetres() {
  const { t, i18n } = useTranslation()
  const [page, setPage] = useState(1)
  const [taille, setTaille] = useState(20)
  const q = usePerimetresSuspendus(page, taille)
  const lignes = q.data?.contenu ?? []
  const colonnes: Column<PerimetreSuspendu>[] = [
    { key: 'code', header: t('sbx.iam.perimetre.code'), render: (p) => <span className="gm-mono">{p.code}</span> },
    { key: 'etat', header: t('sbx.iam.perimetre.etat'), render: () => <StatusBadge status="crit" label={t('sbx.iam.perimetre.suspendu')} /> },
    { key: 'depuis', header: t('sbx.iam.perimetre.depuis'), render: (p) => dateHeure(p.depuis, i18n.language) },
    { key: 'motif', header: t('sbx.iam.perimetre.motif'), render: (p) => p.motif ?? '—' },
    { key: 'acteurId', header: t('sbx.iam.perimetre.acteur'), render: (p) => <span className="gm-mono">{p.acteurId}</span> },
  ]
  return (
    <div className="gm-col">
      <Banner level="info" title={t('sbx.iam.perimetre.aide.titre')}>{t('sbx.iam.perimetre.aide')}</Banner>
      <DataTable
        columns={colonnes}
        rows={lignes}
        rowKey={(p) => p.code}
        total={q.data?.total}
        page={page}
        pageSize={taille}
        onPage={setPage}
        onPageSize={(n) => {
          setTaille(n)
          setPage(1)
        }}
        onRetry={() => void q.refetch()}
        emptyText={t('sbx.iam.perimetre.vide')}
        density="compact"
        {...etatListe(q, lignes)}
      />
    </div>
  )
}

function Protections() {
  const { t, i18n } = useTranslation()
  const [page, setPage] = useState(1)
  const [taille, setTaille] = useState(20)
  const q = useComptesProteges(page, taille)
  const lignes = q.data?.contenu ?? []
  const colonnes: Column<CompteProtege>[] = [
    { key: 'nom', header: t('sbx.iam.protection.compte'), render: (c) => c.nom },
    { key: 'institutionId', header: t('sbx.iam.protection.institution'), render: (c) => <span className="gm-mono">{c.institutionId}</span> },
    { key: 'etat', header: t('sbx.iam.protection.etat'), render: () => <StatusBadge status="ok" icon="shield-check" label={t('sbx.iam.protection.protege')} /> },
    { key: 'depuis', header: t('sbx.iam.protection.depuis'), render: (c) => dateHeure(c.depuis, i18n.language) },
  ]
  return (
    <div className="gm-col">
      <Banner level="info" title={t('sbx.iam.protection.aide.titre')}>{t('sbx.iam.protection.aide')}</Banner>
      <DataTable
        columns={colonnes}
        rows={lignes}
        rowKey={(c) => c.compteId}
        total={q.data?.total}
        page={page}
        pageSize={taille}
        onPage={setPage}
        onPageSize={(n) => {
          setTaille(n)
          setPage(1)
        }}
        onRetry={() => void q.refetch()}
        emptyText={t('sbx.iam.protection.vide')}
        density="compact"
        {...etatListe(q, lignes)}
      />
    </div>
  )
}

export default function SandboxIamPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const demande = params.get('onglet')
  const onglet: OngletSandbox = ONGLETS_SANDBOX.includes(demande as OngletSandbox) ? (demande as OngletSandbox) : 'perimetres'
  return (
    <div className="gm-col">
      <PageHeader title={t('sbx.iam.titre')} subtitle={t('sbx.iam.sousTitre')} />
      <Tabs items={ONGLETS_SANDBOX.map((k) => ({ key: k, label: t(`sbx.iam.onglet.${k}`) }))} value={onglet} onChange={(k) => setParams({ onglet: k })} />
      {onglet === 'perimetres' ? <Perimetres /> : <Protections />}
    </div>
  )
}
