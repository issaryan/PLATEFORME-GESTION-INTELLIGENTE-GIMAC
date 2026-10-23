import { useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Banner, DataTable, StatusBadge, Tag, type Column, type Status } from '@gimac/design-system'
import type { ApiError } from '@gimac/socle'
import { dateHeure } from '@gimac/compte'
import { useComptesProteges, useDelegations, usePerimetresSuspendus } from '../hooks/sandbox'
import type { CompteProtege, EtatEffectifDelegation, PageSandbox, PerimetreSuspendu, VueDelegation } from '../modele/sandbox'
import { etatListe } from './formats'

// Onglets de l'écran « Sandbox GIMAC » de la console (D-119) : chaque onglet lit une page de MS-IAM.

type Lecture<T> = { data?: PageSandbox<T>; isPending: boolean; isError: boolean; error: ApiError | null; refetch: () => unknown }

/** Table paginée d'un onglet, avec son aide et son texte de liste vide. */
function TableSandbox<T extends object>(p: {
  lire: (page: number, taille: number) => Lecture<T>
  colonnes: Column<T>[]
  cle: (x: T) => string
  aide: { titre: string; texte: string }
  vide: string
  entete?: ReactNode
}) {
  const [page, setPage] = useState(1)
  const [taille, setTaille] = useState(20)
  const q = p.lire(page, taille)
  const lignes = q.data?.contenu ?? []
  return (
    <div className="gm-col">
      <Banner level="info" title={p.aide.titre}>{p.aide.texte}</Banner>
      {p.entete}
      <DataTable
        columns={p.colonnes}
        rows={lignes}
        rowKey={p.cle}
        total={q.data?.total}
        page={page}
        pageSize={taille}
        onPage={setPage}
        onPageSize={(n) => {
          setTaille(n)
          setPage(1)
        }}
        onRetry={() => void q.refetch()}
        emptyText={p.vide}
        density="compact"
        {...etatListe(q, lignes)}
      />
    </div>
  )
}

export function OngletPerimetres() {
  const { t, i18n } = useTranslation()
  const colonnes: Column<PerimetreSuspendu>[] = [
    { key: 'code', header: t('sbx.iam.perimetre.code'), render: (p) => <span className="gm-mono">{p.code}</span> },
    { key: 'etat', header: t('sbx.iam.perimetre.etat'), render: () => <StatusBadge status="crit" label={t('sbx.iam.perimetre.suspendu')} /> },
    { key: 'depuis', header: t('sbx.iam.perimetre.depuis'), render: (p) => dateHeure(p.depuis, i18n.language) },
    { key: 'motif', header: t('sbx.iam.perimetre.motif'), render: (p) => p.motif ?? '—' },
    { key: 'acteurId', header: t('sbx.iam.perimetre.acteur'), render: (p) => <span className="gm-mono">{p.acteurId}</span> },
  ]
  return (
    <TableSandbox lire={usePerimetresSuspendus} colonnes={colonnes} cle={(p) => p.code}
      aide={{ titre: t('sbx.iam.perimetre.aide.titre'), texte: t('sbx.iam.perimetre.aide') }} vide={t('sbx.iam.perimetre.vide')} />
  )
}

export function OngletProtections() {
  const { t, i18n } = useTranslation()
  const colonnes: Column<CompteProtege>[] = [
    { key: 'nom', header: t('sbx.iam.protection.compte'), render: (c) => c.nom },
    { key: 'institutionId', header: t('sbx.iam.protection.institution'), render: (c) => <span className="gm-mono">{c.institutionId}</span> },
    { key: 'etat', header: t('sbx.iam.protection.etat'), render: () => <StatusBadge status="ok" icon="shield-check" label={t('sbx.iam.protection.protege')} /> },
    { key: 'depuis', header: t('sbx.iam.protection.depuis'), render: (c) => dateHeure(c.depuis, i18n.language) },
  ]
  return (
    <TableSandbox lire={useComptesProteges} colonnes={colonnes} cle={(c) => c.compteId}
      aide={{ titre: t('sbx.iam.protection.aide.titre'), texte: t('sbx.iam.protection.aide') }} vide={t('sbx.iam.protection.vide')} />
  )
}

/** Couleur de l'état effectif d'une délégation. */
export const STATUT_DELEGATION: Record<EtatEffectifDelegation, Status> = {
  ACTIVE: 'ok', PROGRAMMEE: 'info', DEMANDEE: 'warn', ECHUE: 'neutral', REFUSEE: 'neutral', REVOQUEE: 'crit',
}

/** Colonnes communes aux mandats et aux accès de prestataire. */
export function colonnesDelegation(t: (k: string) => string, langue: string, portee: Column<VueDelegation>): Column<VueDelegation>[] {
  return [
    { key: 'nomCompte', header: t('sbx.iam.delegation.titulaire'), render: (v) => v.nomCompte },
    portee,
    { key: 'periode', header: t('sbx.iam.delegation.periode'), render: (v) => `${dateHeure(v.delegation.debut, langue)} → ${dateHeure(v.delegation.fin, langue)}` },
    { key: 'etat', header: t('sbx.iam.delegation.etat'), render: (v) => <StatusBadge status={STATUT_DELEGATION[v.etat]} label={t(`sbx.iam.delegation.etat.${v.etat}`)} /> },
  ]
}

const useMandats = (page: number, taille: number) => useDelegations('MANDAT', page, taille)

export function OngletMandats() {
  const { t, i18n } = useTranslation()
  const portee: Column<VueDelegation> = {
    key: 'institution', header: t('sbx.iam.mandat.mandant'),
    render: (v) => (
      <div className="gm-col" style={{ gap: 4 }}>
        <span className="gm-mono">{v.delegation.institutionId}</span>
        <div className="gm-row" style={{ flexWrap: 'wrap', gap: 4 }}>{v.delegation.permissions.map((p) => <Tag key={p}>{t(`droits.permission.${p}`)}</Tag>)}</div>
      </div>
    ),
  }
  return (
    <TableSandbox lire={useMandats}colonnes={colonnesDelegation(t, i18n.language, portee)} cle={(v) => v.delegation.id}
      aide={{ titre: t('sbx.iam.mandat.aide.titre'), texte: t('sbx.iam.mandat.aide') }} vide={t('sbx.iam.mandat.vide')} />
  )
}
