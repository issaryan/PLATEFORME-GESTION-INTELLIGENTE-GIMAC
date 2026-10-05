import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { DataTable, Link, PageHeader, StatusBadge, type Column } from '@gimac/design-system'
import type { MembreEquipe } from '../../../modele/pilotage'
import { ajouterJours, aujourdhui } from '../../../modele/jours'
import { useEquipe } from '../../../hooks/pilotage'
import { etatListe } from '../liaisons/communs'
import { BadgeRapport, NavigationPeriode, jourDemande, useDates, useLibelleUnite } from './communs'

// Mon équipe (D-76, /des/equipe, PIL_TEAM_READ) : les comptes dont l'utilisateur figure dans la chaîne des
// responsables (MS-IAM), avec, pour le jour choisi, leurs activités, leur rapport du jour, leurs tâches
// ouvertes et en retard, et leur absence (événement ABSENCE) ; accès à leur agenda, leur journée et leurs
// rapports. Lecture seule : les compteurs viennent de MS-PILOTAGE.

export default function EquipePage() {
  const { t } = useTranslation()
  const dates = useDates()
  const libelleUnite = useLibelleUnite()
  const [params, setParams] = useSearchParams()
  const jour = jourDemande(params.get('jour'))
  const equipe = useEquipe(jour)
  const lignes = equipe.data ?? []
  const aller = (j: string | undefined) => setParams(j ? { jour: j } : {})
  const colonnes: Column<MembreEquipe>[] = [
    {
      key: 'collaborateur', header: t('act.equipe.collaborateur'), render: (m) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <strong>{m.collaborateur.nomAffiche}</strong>
          <span className="gm-field-help">{[m.collaborateur.fonction, libelleUnite(m.collaborateur.unite)].filter(Boolean).join(' · ')}</span>
          {m.collaborateur.niveau > 1 ? <span className="gm-field-help">{t('act.equipe.niveau', { niveau: m.collaborateur.niveau })}</span> : null}
        </span>
      ),
    },
    {
      key: 'activites', header: t('act.equipe.activites'), render: (m) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <span>{t('act.equipe.faites', { faites: m.activites.faites, prevues: m.activites.prevues })}</span>
          {m.activites.enCours ? <span className="gm-field-help">{t('act.equipe.enCours', { count: m.activites.enCours })}</span> : null}
        </span>
      ),
    },
    { key: 'rapport', header: t('act.equipe.rapport'), render: (m) => <BadgeRapport etat={m.rapportDuJour} /> },
    {
      key: 'taches', header: t('act.equipe.taches'), render: (m) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-1)' }}>
          <span>{m.tachesOuvertes}</span>
          {m.tachesEnRetard ? <StatusBadge status="crit" icon="clock" label={t('act.equipe.enRetard', { count: m.tachesEnRetard })} /> : null}
        </span>
      ),
    },
    { key: 'presence', header: t('act.equipe.presence'), render: (m) => <StatusBadge status={m.absent ? 'warn' : 'ok'} label={t(m.absent ? 'act.equipe.absent' : 'act.equipe.present')} /> },
    {
      key: 'acces', header: t('act.equipe.acces'), render: (m) => (
        <span className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
          <Link href={`/des/activite?jour=${jour}&collaborateur=${encodeURIComponent(m.collaborateur.id)}`}>{t('act.equipe.journee')}</Link>
          <Link href={`/des/agenda?jour=${jour}&collaborateur=${encodeURIComponent(m.collaborateur.id)}`}>{t('act.equipe.agenda')}</Link>
          <Link href={`/des/rapports?vue=recus&auteur=${encodeURIComponent(m.collaborateur.id)}`}>{t('act.equipe.rapports')}</Link>
        </span>
      ),
    },
  ]
  return (
    <div className="gm-col">
      <PageHeader title={t('act.equipe.titre')} subtitle={t('act.equipe.sousTitre')} />
      <NavigationPeriode
        libelle={dates.jourLong(jour)}
        precedent={t('act.jourPrecedent')}
        suivant={t('act.jourSuivant')}
        aujourdhuiLibelle={t('act.aujourdhui')}
        onPrecedent={() => aller(ajouterJours(jour, -1))}
        onSuivant={() => aller(ajouterJours(jour, 1))}
        onAujourdhui={jour !== aujourdhui() ? () => aller(undefined) : undefined}
      />
      <DataTable columns={colonnes} rows={lignes} rowKey={(m) => m.collaborateur.id} emptyText={t('act.equipe.vide')} emptyHelp={t('act.equipe.videAide')}
        {...etatListe(equipe, lignes)} onRetry={() => void equipe.refetch()} />
    </div>
  )
}
