import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button, DataTable, FilterBar, Link, StatusBadge, Tag, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { useLibelleProfil, type Compte } from '@gimac/compte'
import { useComptes, useProfils } from '../hooks'
import { ETATS_FILTRE, type EtatFiltre, type FiltreComptes } from '../modele/comptes'
import { etatListe } from '../composants/formats'

// Comptes (D-62) : liste paginée côté serveur, recherche libre, filtres par profil et par état (valeurs
// acceptées par MS-IAM : ACTIF, INACTIF, PREMIER_ACCES, SANS_2FA). Aucune inscription libre : la
// création d'un compte est réservée à IAM_ACCOUNT_CREATE.
export default function ComptesPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const permissions = usePermissions()
  const libelleProfil = useLibelleProfil()
  const [filtre, setFiltre] = useState<FiltreComptes>({ page: 1, taille: 25 })
  const liste = useComptes(filtre)
  const profils = useProfils()
  const lignes = liste.data?.contenu ?? []

  const colonnes: Column<Compte>[] = [
    { key: 'identifiant', header: t('compte.identifiant'), render: (c) => <Link href={`/console/comptes/${c.id}`} mono>{c.identifiant}</Link> },
    { key: 'nomAffiche', header: t('console.comptes.nom') },
    { key: 'email', header: t('compte.email'), render: (c) => c.email ?? '—' },
    { key: 'profils', header: t('compte.profils'), render: (c) => <span className="gm-row">{c.profils.map((p) => <Tag key={p}>{libelleProfil(p)}</Tag>)}</span> },
    {
      key: 'actif', header: t('console.comptes.etat'),
      render: (c) => <StatusBadge status={c.actif ? 'ok' : 'neutral'} label={t(c.actif ? 'console.comptes.actif' : 'console.comptes.inactif')} />,
    },
    {
      key: 'securite', header: t('console.comptes.securite'),
      render: (c) => (
        <span className="gm-row">
          {c.premierAccesEnAttente ? <StatusBadge status="info" label={t('console.comptes.premierAcces')} /> : null}
          {!c.secondFacteur ? <StatusBadge status="warn" label={t('console.comptes.sans2fa')} /> : null}
          {!c.premierAccesEnAttente && c.secondFacteur ? <StatusBadge status="ok" label={t('console.comptes.conforme')} /> : null}
        </span>
      ),
    },
  ]
  const changer = (cle: string, v: string) => setFiltre((f) => ({ ...f, page: 1, [cle]: v || undefined }))

  return (
    <div className="gm-col">
      <div className="gm-resource-head">
        <h1 className="gm-heading-display">{t('console.comptes.titre')}</h1>
        {aPermission(permissions, 'IAM_ACCOUNT_CREATE') ? (
          <Button variant="primary" icon="plus" onClick={() => void navigate('/console/comptes/nouveau')}>{t('console.comptes.creer')}</Button>
        ) : null}
      </div>
      <FilterBar
        query={filtre.q ?? ''}
        onQuery={(q) => changer('q', q)}
        filters={[
          { key: 'profil', label: t('console.comptes.filtre.profil'), value: filtre.profil, options: (profils.data ?? []).map((p) => ({ value: p.code, label: libelleProfil(p.code) })) },
          { key: 'etat', label: t('console.comptes.filtre.etat'), value: filtre.etat, options: ETATS_FILTRE.map((e) => ({ value: e, label: t(`console.comptes.filtre.etat.${e}`) })) },
        ]}
        onChange={(k, v) => (k === 'etat' ? changer('etat', v as EtatFiltre) : changer(k, v))}
        onReset={() => setFiltre({ page: 1, taille: filtre.taille })}
      />
      <DataTable
        columns={colonnes}
        rows={lignes}
        rowKey={(c) => c.id}
        total={liste.data?.total}
        page={filtre.page}
        pageSize={filtre.taille}
        onPage={(page) => setFiltre((f) => ({ ...f, page }))}
        onPageSize={(taille) => setFiltre((f) => ({ ...f, taille, page: 1 }))}
        onRowOpen={(c) => void navigate(`/console/comptes/${c.id}`)}
        onRetry={() => void liste.refetch()}
        emptyText={t('console.comptes.aucun')}
        emptyHelp={t('console.comptes.aucun.aide')}
        {...etatListe(liste, lignes)}
        toolbar={<span className="gm-num">{t('console.comptes.total', { n: liste.data?.total ?? 0 })}</span>}
      />
    </div>
  )
}
