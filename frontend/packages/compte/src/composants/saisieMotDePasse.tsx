import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { TextField } from '@gimac/design-system'
import { RappelPolitique, useLibelleViolation, violationsDe } from '@gimac/socle'
import { useChangerMotDePasse } from '../hooks'
import { useMessageErreur } from '../outils'
import type { ErreurModale } from './ModaleSaisie'

// Changement du mot de passe sur la plateforme (D-65) : premier accès (mot de passe provisoire) et
// « Mon compte » (modale de saisie). Champs, validation locale et envoi ; le conteneur fournit le
// formulaire (étape de l'assistant ou modale). Les saisies ne vivent que dans cet état et sont effacées
// après chaque réponse ; le nouveau mot de passe n'est remis qu'à l'appelant, jamais rangé.
export function useSaisieMotDePasse({ provisoire = false, onFait }: { provisoire?: boolean; onFait: (nouveau: string) => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const libelleViolation = useLibelleViolation()
  const changer = useChangerMotDePasse()
  const [actuel, setActuel] = useState('')
  const [nouveau, setNouveau] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [champs, setChamps] = useState<{ actuel?: string; nouveau?: string; confirmation?: string }>({})
  const [erreur, setErreur] = useState<ErreurModale | null>(null)

  const soumettre = async () => {
    setErreur(null)
    const locales = {
      actuel: actuel ? undefined : t('motDePasse.actuel.requis'),
      nouveau: nouveau ? undefined : t('motDePasse.nouveau.requis'),
      confirmation: confirmation === nouveau ? undefined : t('motDePasse.confirmation.differente'),
    }
    setChamps(locales)
    if (locales.actuel || locales.nouveau || locales.confirmation) return
    const choisi = nouveau
    try {
      await changer.mutateAsync({ actuel, nouveau: choisi })
      onFait(choisi)
    } catch (x) {
      const m = message(x)
      const violations = violationsDe(x)
      if (m.code === 'IAM_PASSWORD_INVALID') setChamps({ actuel: m.texte })
      else if (m.code === 'IAM_PASSWORD_REUSED') setChamps({ nouveau: m.texte })
      else setErreur({ title: m.texte, correlationId: m.correlationId, text: violations.length ? `${t('motDePasse.violations')} ${violations.map(libelleViolation).join(' ; ')}.` : undefined })
    } finally {
      setActuel('')
      setNouveau('')
      setConfirmation('')
    }
  }

  const contenu = (
    <>
      <TextField name="motDePasseActuel" type="password" label={t(provisoire ? 'motDePasse.provisoire' : 'motDePasse.actuel')} value={actuel} onChange={setActuel}
        required autoComplete="current-password" error={champs.actuel} />
      <TextField name="nouveauMotDePasse" type="password" label={t('motDePasse.nouveau')} value={nouveau} onChange={setNouveau} required autoComplete="new-password" error={champs.nouveau} />
      <RappelPolitique />
      <TextField name="confirmationMotDePasse" type="password" label={t('motDePasse.confirmation')} value={confirmation} onChange={setConfirmation}
        required autoComplete="new-password" error={champs.confirmation} />
    </>
  )
  return { contenu, soumettre, enCours: changer.isPending, erreur, modifie: !!(actuel || nouveau || confirmation) }
}
