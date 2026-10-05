import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Card, InlineAlert, Link, LinkList, PageSection, Skeleton, StatGrid, type StatCardProps } from '@gimac/design-system'
import { aujourdhui, jourDe } from '../../../modele/jours'
import { useSynthese } from '../../../hooks/pilotage'
import { TagCategorie, useDates, useErreur } from '../activite/communs'

// « Ma journée » du tableau de bord (D-76 §2.6, §6) : synthèse de MS-PILOTAGE pour tout porteur de
// PIL_ACTIVITY_OWN, avant les sections métier : activités du jour, tâches ouvertes et en retard, rapport du
// jour, rapports à viser, cinq prochains événements. Les compteurs viennent du service.

export function SectionMaJournee({ equipe }: { equipe: boolean }) {
  const { t } = useTranslation()
  const dates = useDates()
  const erreur = useErreur()
  const navigate = useNavigate()
  const jour = aujourdhui()
  const synthese = useSynthese(jour)
  const s = synthese.data
  const vers = (chemin: string) => () => void navigate(chemin)
  const tuiles: (StatCardProps & { key: string })[] = s ? [
    { key: 'activites', label: t('act.synthese.activites'), value: `${s.activites.faites} / ${s.activites.prevues}`, context: t('act.synthese.enCours', { count: s.activites.enCours }), onClick: vers('/des/activite') },
    {
      key: 'taches', label: t('act.synthese.taches'), value: s.taches.ouvertes, status: s.taches.enRetard ? 'crit' : s.taches.echeanceProche ? 'warn' : 'ok',
      statusLabel: s.taches.enRetard ? t('act.synthese.enRetard', { count: s.taches.enRetard }) : s.taches.echeanceProche ? t('act.synthese.echeanceProche', { count: s.taches.echeanceProche }) : t('tableau.aJour'),
      onClick: vers('/des/taches'),
    },
    {
      key: 'rapport', label: t('act.synthese.rapport'), value: t(`act.rapport.etat.${s.rapportDuJour}`), status: s.rapportDuJour === 'ABSENT' ? 'warn' : 'ok',
      statusLabel: t(s.rapportDuJour === 'ABSENT' ? 'act.synthese.aRediger' : 'tableau.aJour'), onClick: vers('/des/activite'),
    },
    ...(equipe || s.rapportsARecevoir > 0 ? [{
      key: 'recus', label: t('act.synthese.recus'), value: s.rapportsARecevoir, status: s.rapportsARecevoir > 0 ? 'warn' as const : 'ok' as const,
      statusLabel: t(s.rapportsARecevoir > 0 ? 'tableau.aTraiter' : 'tableau.aJour'), onClick: vers('/des/rapports?vue=recus'),
    }] : []),
  ] : []
  return (
    <PageSection title={t('act.synthese.titre')} description={dates.jourLong(jour)} actions={<Link href="/des/activite">{t('act.synthese.ouvrir')}</Link>}>
      {synthese.isPending ? <Skeleton height={96} /> : synthese.isError || !s ? (
        <InlineAlert level="crit" correlationId={synthese.error?.correlationId || undefined}>{erreur(synthese.error)?.titre}</InlineAlert>
      ) : (
        <>
          <StatGrid label={t('act.synthese.titre')} items={tuiles} />
          <Card title={t('act.synthese.evenements')} actions={<Link href="/des/agenda">{t('act.journee.ouvrirAgenda')}</Link>}>
            <LinkList label={t('act.synthese.evenements')} pagination={false} empty={t('act.synthese.aucunEvenement')}
              items={s.evenements.map((e) => ({
                key: e.id, href: `/des/agenda?jour=${jourDe(e.debut)}&evenement=${e.id}`,
                label: `${dates.jourCourt(jourDe(e.debut))}${e.journeeEntiere ? '' : `, ${dates.heure(e.debut)}`} · ${e.titre}`, meta: <TagCategorie categorie={e.categorie} />,
              }))} />
          </Card>
        </>
      )}
    </PageSection>
  )
}
