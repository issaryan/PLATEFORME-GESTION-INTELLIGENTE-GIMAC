import { useCallback, useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation } from 'react-router'
import { Button, IconButton, Toast, ToastRegion } from '@gimac/design-system'
import { TYPES_MESSAGERIE, type NotificationPlateforme } from './modele'
import { ICONES_NOTIFICATION } from './Cloche'
import {
  conversationOuverte, useApercuNotification, useContexteNotifications, useMarquerNotificationLue, useNotificationsRecentes, useOuvrirNotification, usePreferencesNotifications,
} from './contexte'
import { jouerSon, notifierSysteme } from './systeme'

// Arrivée d'une notification (D-80, docs/lot1/notifications.md §4) : détectée en comparant les notifications
// récentes, relues à chaque événement du canal poussé (et du service simulé), à celles déjà vues (identifiant
// et misAJourLe : une création ou un regroupement). Onglet visible : fenêtre surgissante (Toast du catalogue,
// 6 s, trois empilées au plus, « Ouvrir »), sauf pour la conversation actuellement ouverte, alors marquée lue.
// Onglet caché : notification du système si la préférence `systeme` est active et la permission accordée.
// Son discret si la préférence `son` est active.

export const DUREE_SURGISSANTE_MS = 6_000
export const SURGISSANTES_MAX = 3

function Surgissante({ n, onFermer, onOuvrir }: { n: NotificationPlateforme; onFermer: () => void; onOuvrir: () => void }) {
  const { t } = useTranslation()
  const apercu = useApercuNotification(n)
  // Six secondes à compter de l'arrivée, quels que soient les rendus intermédiaires.
  const fermer = useRef(onFermer)
  fermer.current = onFermer
  useEffect(() => {
    const minuterie = window.setTimeout(() => fermer.current(), DUREE_SURGISSANTE_MS)
    return () => window.clearTimeout(minuterie)
  }, [])
  const messagerie = TYPES_MESSAGERIE.includes(n.type)
  const texte = messagerie ? apercu.data : n.sousTitre
  return (
    <Toast className="gm-toast-notif" icon={n.type === 'MESSAGE' ? 'message-square' : ICONES_NOTIFICATION[n.type]}
      action={
        <span className="gm-row" style={{ gap: 'var(--gm-space-1)' }}>
          <Button size="sm" variant="secondary" onClick={onOuvrir}>{t('notifications.ouvrir')}</Button>
          <IconButton size="sm" variant="ghost" icon="x" label={t('notifications.fermer')} onClick={onFermer} />
        </span>
      }>
      <span className="gm-toast-titre">{n.nombre > 1 ? `${n.titre} (${n.nombre})` : n.titre}</span>
      {messagerie && n.sousTitre ? <span className="gm-toast-texte">{n.sousTitre}</span> : null}
      {texte ? <span className="gm-toast-texte">{texte}</span> : null}
    </Toast>
  )
}

export function NotificationsSurgissantes() {
  const { t } = useTranslation()
  const c = useContexteNotifications()
  const ici = useLocation()
  const recentes = useNotificationsRecentes()
  const preferences = usePreferencesNotifications()
  const ouvrir = useOuvrirNotification()
  const lue = useMarquerNotificationLue()
  const vues = useRef<Map<string, string> | null>(null)
  const ouverture = useRef(new Date().toISOString())
  const [affichees, setAffichees] = useState<NotificationPlateforme[]>([])
  // Valeurs courantes lues par l'effet d'arrivée sans le relancer.
  const etat = useRef({ ici, preferences: preferences.data })
  etat.current = { ici, preferences: preferences.data }

  useEffect(() => {
    const liste = recentes.data?.contenu
    if (!liste || !c) return
    // Premier chargement : l'état connu, sans fenêtre surgissante, sauf ce qui est arrivé depuis l'ouverture de l'écran
    // (QUO-11 : une notification reçue avant la première lecture de la liste n'était jamais annoncée).
    if (!vues.current) {
      vues.current = new Map(liste.filter((n) => n.lueLe || n.misAJourLe <= ouverture.current).map((n) => [n.id, n.misAJourLe]))
    }
    const connues = vues.current
    const arrivees = liste.filter((n) => !n.lueLe && (connues.get(n.id) ?? '') < n.misAJourLe).reverse()
    liste.forEach((n) => connues.set(n.id, n.misAJourLe))
    const { ici: emplacement, preferences: prefs } = etat.current
    for (const n of arrivees) {
      if (document.hidden) {
        if (prefs?.systeme) {
          void Promise.resolve(TYPES_MESSAGERIE.includes(n.type) && c.apercu ? c.apercu(n).catch(() => null) : n.sousTitre)
            .then((corps) => notifierSysteme({ titre: n.titre, corps, etiquette: n.id, ouvrir: () => ouvrir(n) }))
        }
      } else if (conversationOuverte(n, c.couche, emplacement)) {
        // Conversation sous les yeux : la notification est lue, sans fenêtre surgissante ni son.
        lue.mutate(n.id)
        continue
      } else {
        setAffichees((a) => [...a.filter((x) => x.id !== n.id), n].slice(-SURGISSANTES_MAX))
      }
      if (prefs?.son) jouerSon()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recentes.data, c])

  const retirer = useCallback((id: string) => setAffichees((a) => a.filter((x) => x.id !== id)), [])
  if (!affichees.length) return null
  return (
    <div role="region" aria-label={t('notifications.surgissantes')}>
      <ToastRegion>
        {affichees.map((n) => (
          <Surgissante key={`${n.id}-${n.misAJourLe}`} n={n} onFermer={() => retirer(n.id)} onOuvrir={() => { retirer(n.id); ouvrir(n) }} />
        ))}
      </ToastRegion>
    </div>
  )
}
