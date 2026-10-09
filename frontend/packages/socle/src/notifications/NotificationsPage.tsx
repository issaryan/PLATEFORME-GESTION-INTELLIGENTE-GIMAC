import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, EmptyState, InlineAlert, PageHeader, Pagination, Skeleton, Tabs } from '@gimac/design-system'
import type { EtatNotifications } from './modele'
import { ElementNotification } from './Cloche'
import { useCompteurNotifications, useContexteNotifications, useListeNotifications, useOuvrirNotification, useToutMarquerLu } from './contexte'

// Page des notifications (/{couche}/notifications, D-80) : « Voir tout » de la cloche. Filtre non lues ou
// toutes, pagination par le service (20 par page, la première à 0), « Tout marquer comme lu ».

export const TAILLE_PAGE_NOTIFICATIONS = 20

export function NotificationsPage() {
  const { t } = useTranslation()
  const [etat, setEtat] = useState<EtatNotifications>('non-lues')
  const [page, setPage] = useState(1)
  const liste = useListeNotifications(etat, page - 1, TAILLE_PAGE_NOTIFICATIONS)
  const compteur = useCompteurNotifications()
  const toutLu = useToutMarquerLu()
  const ouvrir = useOuvrirNotification()
  const nonLues = compteur.data?.nonLues ?? 0
  // Portail : le participant ne reçoit que le parcours de ses liaisons (ni messagerie, ni tâches, ni rapports).
  const portail = useContexteNotifications()?.couche === 'portail'
  const contenu = liste.data?.contenu ?? []
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-4)' }}>
      <PageHeader title={t('notifications.titre')} subtitle={t(portail ? 'notifications.page.sousTitrePortail' : 'notifications.page.sousTitre')}
        meta={<span>{t('notifications.nonLues', { count: nonLues })}</span>}
        actions={<Button variant="secondary" icon="check" disabled={nonLues === 0} loading={toutLu.isPending} onClick={() => toutLu.mutate(undefined)}>{t('notifications.toutLu')}</Button>} />
      <Card>
        <Tabs items={[{ key: 'non-lues', label: t('notifications.filtre.nonLues') }, { key: 'toutes', label: t('notifications.filtre.toutes') }]} value={etat}
          onChange={(k) => { setEtat(k as EtatNotifications); setPage(1) }} />
        {liste.isPending ? (
          <Skeleton height={240} />
        ) : liste.isError ? (
          <InlineAlert level="crit">{t('notifications.erreur')}</InlineAlert>
        ) : contenu.length === 0 ? (
          <EmptyState icon="bell-ring" title={t(etat === 'non-lues' ? 'notifications.aucuneNonLue' : 'notifications.aucune')} />
        ) : (
          <>
            <ul className="gm-cloche-liste gm-notifications-page" aria-label={t('notifications.titre')}>
              {contenu.map((n) => <ElementNotification key={n.id} n={n} onOuvrir={ouvrir} />)}
            </ul>
            <Pagination total={liste.data?.total ?? 0} page={page} pageSize={TAILLE_PAGE_NOTIFICATIONS} onPage={setPage} />
          </>
        )}
      </Card>
    </div>
  )
}
