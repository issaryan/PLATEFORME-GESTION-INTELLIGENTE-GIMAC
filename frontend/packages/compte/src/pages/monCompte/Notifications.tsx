import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation } from 'react-router'
import { Button, Card, InlineAlert, Note, Skeleton, StatusBadge, Switch } from '@gimac/design-system'
import {
  TYPES_NOTIFICATION, demanderNotificationSysteme, etatNotificationSysteme, useEnregistrerPreferencesNotifications, useNotificationsDisponibles, usePreferencesNotifications,
  type EtatSysteme, type PreferencesNotifications,
} from '@gimac/socle'
import { Annonce, type Retour } from '../../composants/Annonce'
import { useMessageErreur } from '../../outils'

// Préférences des notifications de la plateforme (D-80, docs/lot1/notifications.md §3, §4), section de
// « Mon compte » dans les couches internes (application interne et console) : types notifiés, notification du
// système et son. Chaque bascule enregistre aussitôt (PUT /preferences, le seul champ changé). La permission
// du navigateur n'est demandée que par le bouton de cette section ; hors contexte sécurisé (http par l'adresse
// IP), la notification du système est indiquée indisponible (HTTPS exigé).

function EtatNavigateur({ etat, onDemander, demande }: { etat: EtatSysteme; onDemander: () => void; demande: boolean }) {
  const { t } = useTranslation()
  if (etat === 'https') return <InlineAlert level="warn" title={t('compte.notifications.systeme.https')} />
  if (etat === 'absente') return <Note>{t('compte.notifications.systeme.absente')}</Note>
  if (etat === 'refusee') return <Note icon="lock">{t('compte.notifications.systeme.refusee')}</Note>
  if (etat === 'accordee') return <span><StatusBadge status="ok" label={t('compte.notifications.systeme.accordee')} /></span>
  return (
    <div className="gm-row" style={{ alignItems: 'center', flexWrap: 'wrap' }}>
      <Button size="sm" icon="bell-ring" loading={demande} onClick={onDemander}>{t('compte.notifications.systeme.autoriser')}</Button>
      <span className="gm-field-help">{t('compte.notifications.systeme.aDemander')}</span>
    </div>
  )
}

function Preferences({ p }: { p: PreferencesNotifications }) {
  const { t } = useTranslation()
  const enregistrer = useEnregistrerPreferencesNotifications()
  const erreur = useMessageErreur()
  const [retour, setRetour] = useState<Retour>(null)
  const [etat, setEtat] = useState<EtatSysteme>(() => etatNotificationSysteme())
  const [demande, setDemande] = useState(false)
  const changer = (modif: Partial<PreferencesNotifications>) => {
    setRetour(null)
    enregistrer.mutate(modif, {
      onSuccess: () => setRetour({ niveau: 'info', message: { texte: t('compte.notifications.enregistrees') } }),
      onError: (e) => setRetour({ niveau: 'crit', message: erreur(e) }),
    })
  }
  const demander = () => {
    setDemande(true)
    void demanderNotificationSysteme().then(setEtat, () => setEtat(etatNotificationSysteme())).finally(() => setDemande(false))
  }
  const indisponible = etat === 'https' || etat === 'absente'
  return (
    <div className="gm-col">
      <div className="gm-col" role="group" aria-label={t('compte.notifications.types')} style={{ gap: 'var(--gm-space-2)' }}>
        <strong>{t('compte.notifications.types')}</strong>
        {/* Un service antérieur ne renvoie ni MENTION, REPONSE (D-82), DEMANDE (D-81) ni PROCESSUS (D-87) : actifs par défaut, comme le contrat. */}
        {TYPES_NOTIFICATION.map((type) => (
          <Switch key={type} label={t(`compte.notifications.type.${type}`)} checked={p.types[type] ?? true} disabled={enregistrer.isPending}
            onChange={(v) => changer({ types: { ...p.types, [type]: v } })} />
        ))}
        <span className="gm-field-help">{t('compte.notifications.types.aide')}</span>
      </div>
      <div className="gm-col" role="group" aria-label={t('compte.notifications.alertes')} style={{ gap: 'var(--gm-space-2)' }}>
        <strong>{t('compte.notifications.alertes')}</strong>
        <Switch label={t('compte.notifications.systeme')} checked={p.systeme && !indisponible} disabled={enregistrer.isPending || indisponible} onChange={(v) => changer({ systeme: v })} />
        <EtatNavigateur etat={etat} demande={demande} onDemander={demander} />
        <Switch label={t('compte.notifications.son')} checked={p.son} disabled={enregistrer.isPending} onChange={(v) => changer({ son: v })} />
      </div>
      <Annonce retour={retour} />
    </div>
  )
}

/** Section « Notifications » de Mon compte ; absente sans notifications dans la couche (portail des participants). */
export function PreferencesNotificationsSection() {
  const { t } = useTranslation()
  const disponibles = useNotificationsDisponibles()
  const prefs = usePreferencesNotifications()
  const ici = useLocation()
  const ancre = useRef<HTMLElement>(null)
  // « Préférences » de la cloche : /{couche}/mon-compte#notifications amène à cette section.
  useEffect(() => {
    if (ici.hash === '#notifications' && prefs.data) ancre.current?.scrollIntoView?.({ block: 'start' })
  }, [ici.hash, prefs.data])
  if (!disponibles) return null
  return (
    <section id="notifications" ref={ancre} aria-label={t('compte.notifications.titre')}>
      <Card title={t('compte.notifications.titre')}>
        {prefs.isPending ? <Skeleton height={160} /> : prefs.isError ? <InlineAlert level="crit" title={t('compte.notifications.erreur')} /> : <Preferences p={prefs.data} />}
      </Card>
    </section>
  )
}
