import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button, Icon, Skeleton, type IconName } from '@gimac/design-system'
import type { NotificationPlateforme, TypeNotification } from './modele'
import { useApercuNotification, useCompteurNotifications, useContexteNotifications, useNotificationsRecentes, useOuvrirNotification, useToutMarquerLu } from './contexte'

// Cloche de la barre du haut (D-80, docs/lot1/notifications.md §4) : panneau des notifications récentes
// (pictogramme du type, titre, sous-titre, aperçu d'un message, depuis, nombre regroupé), « Tout marquer
// comme lu » et « Voir tout ». Un clic ouvre le lien de la notification et la marque lue.

export const ICONES_NOTIFICATION: Record<TypeNotification, IconName> = { MESSAGE: 'messages-square', TACHE: 'square-check', RAPPORT: 'file-text', MENTION: 'at-sign', REPONSE: 'reply', DEMANDE: 'inbox', PROCESSUS: 'workflow', HOMOLOGATION: 'route', MONITORING: 'calendar-clock' }

/** « à l'instant », « il y a 5 min », « hier »… dans la langue de l'interface. */
export function depuis(iso: string, langue: string, maintenant = Date.now()): string | null {
  const ecart = Math.round((new Date(iso).getTime() - maintenant) / 1000)
  if (Number.isNaN(ecart)) return null
  if (Math.abs(ecart) < 60) return null
  const f = new Intl.RelativeTimeFormat(langue, { numeric: 'auto', style: 'short' })
  const minutes = Math.round(ecart / 60)
  if (Math.abs(minutes) < 60) return f.format(minutes, 'minute')
  const heures = Math.round(minutes / 60)
  if (Math.abs(heures) < 24) return f.format(heures, 'hour')
  return f.format(Math.round(heures / 24), 'day')
}

export function useDepuis() {
  const { t, i18n } = useTranslation()
  return (iso: string) => depuis(iso, i18n.language) ?? t('notifications.instant')
}

/** Une notification de la liste : bouton qui l'ouvre ; non lue, elle porte un filet et le fond d'action. */
export function ElementNotification({ n, onOuvrir }: { n: NotificationPlateforme; onOuvrir: (n: NotificationPlateforme) => void }) {
  const { t } = useTranslation()
  const apercu = useApercuNotification(n)
  const quand = useDepuis()
  return (
    <li>
      <button type="button" className={`gm-cloche-item${n.lueLe ? '' : ' gm-notif-unread'}`} onClick={() => onOuvrir(n)}
        aria-label={`${t(`notifications.type.${n.type}`)} : ${n.titre}${n.lueLe ? '' : `, ${t('notifications.nonLue')}`}`}>
        <Icon name={ICONES_NOTIFICATION[n.type]} size={20} />
        <span className="gm-cloche-corps">
          <span className="gm-cloche-ligne">
            <span className="gm-cloche-titre">{n.titre}</span>
            <span className="gm-cloche-depuis">{quand(n.misAJourLe)}</span>
          </span>
          {n.sousTitre ? <span className="gm-field-help">{n.sousTitre}</span> : null}
          {apercu.data ? <span className="gm-cloche-apercu">{apercu.data}</span> : null}
          {n.nombre > 1 ? <span><span className="gm-badge gm-badge-info gm-num">{t('notifications.nombre', { count: n.nombre })}</span></span> : null}
        </span>
      </button>
    </li>
  )
}

/** Contenu du panneau ancré à la cloche ; `fermer` le referme à l'ouverture d'une notification. */
export function PanneauNotifications({ fermer }: { fermer: () => void }) {
  const { t } = useTranslation()
  const c = useContexteNotifications()
  const navigate = useNavigate()
  const recentes = useNotificationsRecentes()
  const compteur = useCompteurNotifications()
  const toutLu = useToutMarquerLu()
  const ouvrir = useOuvrirNotification()
  const nonLues = compteur.data?.nonLues ?? 0
  const aller = (chemin: string) => {
    fermer()
    void navigate(chemin)
  }
  const liste = recentes.data?.contenu ?? []
  return (
    <>
      <div className="gm-cloche-tete">
        <strong>{t('notifications.titre')}</strong>
        <span className="gm-field-help">{t('notifications.nonLues', { count: nonLues })}</span>
        <span style={{ flex: 1 }} />
        <Button size="sm" variant="ghost" icon="check" disabled={nonLues === 0} loading={toutLu.isPending} onClick={() => toutLu.mutate(undefined)}>{t('notifications.toutLu')}</Button>
      </div>
      {recentes.isPending ? (
        <div className="gm-pad"><Skeleton height={120} /></div>
      ) : recentes.isError ? (
        <p className="gm-cloche-vide">{t('notifications.erreur')}</p>
      ) : liste.length === 0 ? (
        <p className="gm-cloche-vide">{t('notifications.aucune')}</p>
      ) : (
        <ul className="gm-cloche-liste" aria-label={t('notifications.recentes')}>
          {liste.map((n) => <ElementNotification key={n.id} n={n} onOuvrir={(x) => { fermer(); ouvrir(x) }} />)}
        </ul>
      )}
      <div className="gm-cloche-pied">
        <Button size="sm" variant="ghost" icon="user" onClick={() => aller(`/${c?.couche ?? 'des'}/mon-compte#notifications`)}>{t('notifications.preferences')}</Button>
        <Button size="sm" variant="secondary" onClick={() => aller(`/${c?.couche ?? 'des'}/notifications`)}>{t('notifications.voirTout')}</Button>
      </div>
    </>
  )
}
