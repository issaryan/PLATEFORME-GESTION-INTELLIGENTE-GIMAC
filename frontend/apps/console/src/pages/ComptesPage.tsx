import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router'
import { Button, DataTable, FilterBar, Link, PageHeader, StatusBadge, Tag, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { useLibelleProfil, type Compte } from '@gimac/compte'
import { useComptes, useProfils } from '../hooks'
import { ETATS_FILTRE, type EtatFiltre, type FiltreComptes } from '../modele/comptes'
import { etatListe } from '../composants/formats'
import { DialogueCreationCompte } from '../composants/DialogueCreationCompte'

// Comptes (D-62) : liste paginée côté serveur, recherche libre, filtres par profil et par état (valeurs
// acceptées par MS-IAM : ACTIF, INACTIF, PREMIER_ACCES, SANS_2FA). Aucune inscription libre : la
// création d'un compte est réservée à IAM_ACCOUNT_CREATE. Filtre d'état initial lu dans l'adresse (?etat=),
// cible des indicateurs du tableau de bord. La création s'ouvre dans une fenêtre modale sur la liste
// (/console/comptes/nouveau ou le bouton « Créer un compte »).
export default function ComptesPage({ creation = false }: { creation?: boolean }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const permissions = usePermissions()
  const libelleProfil = useLibelleProfil()
  const [params] = useSearchParams()
  const etatDemande = params.get('etat') as EtatFiltre | null
  const etatValide = etatDemande && ETATS_FILTRE.includes(etatDemande) ? etatDemande : undefined
  const [filtre, setFiltre] = useState<FiltreComptes>({ page: 1, taille: 25, etat: etatValide })
  // Indicateur suivi depuis le tableau de bord alors que la liste est déjà affichée : le filtre suit l'adresse.
  const [etatSuivi, setEtatSuivi] = useState(etatValide)
  if (etatSuivi !== etatValide) {
    setEtatSuivi(etatValide)
    setFiltre((f) => ({ ...f, page: 1, etat: etatValide }))
  }
  const [creer, setCreer] = useState(creation)
  const fermerCreation = () => {
    setCreer(false)
    if (creation) void navigate('/console/comptes', { replace: true })
  }
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
      <PageHeader title={t('console.comptes.titre')} subtitle={t('console.comptes.sousTitre')}
        actions={aPermission(permissions, 'IAM_ACCOUNT_CREATE') ? <Button variant="primary" icon="plus" onClick={() => setCreer(true)}>{t('console.comptes.creer')}</Button> : undefined} />
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
      {creer && aPermission(permissions, 'IAM_ACCOUNT_CREATE') ? <DialogueCreationCompte onFermer={fermerCreation} /> : null}
    </div>
  )
}
