import { useCallback, useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, InlineAlert, QrCode, Skeleton, TextField } from '@gimac/design-system'
import type { EnrolementTotp } from '../modele'
import { useConfirmerTotp, useEnrolerTotp } from '../hooks'
import { useMessageErreur, type MessageErreur } from '../outils'

const CODE = /^[0-9]{6}$/

/** Secret base32 par groupes de quatre, pour la saisie manuelle. */
export const grouper = (secret: string) => secret.replace(/(.{4})/g, '$1 ').trim()

// Configuration du code TOTP sur la plateforme (D-65) : MS-IAM tire un secret (gardé 10 minutes), la
// plateforme affiche son code QR calculé localement et, sur demande, le secret en clair ; un premier code
// de l'application d'authentification confirme l'association (le code existant est remplacé). Le
// conteneur fournit le formulaire (étape de l'assistant ou modale de saisie).
export function useSaisieTotp({ onFait, remplacement = false }: { onFait: () => void; remplacement?: boolean }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const enroler = useEnrolerTotp()
  const confirmer = useConfirmerTotp()
  const [enrolement, setEnrolement] = useState<EnrolementTotp | null>(null)
  const [secretVisible, setSecretVisible] = useState(false)
  const [code, setCode] = useState('')
  const [libelle, setLibelle] = useState('')
  const [champ, setChamp] = useState<string | undefined>()
  const [erreur, setErreur] = useState<(MessageErreur & { regenerer?: boolean }) | null>(null)
  const lance = useRef(false)
  const { mutateAsync } = enroler
  const messageRef = useRef(message)
  messageRef.current = message

  const nouveau = useCallback(async () => {
    setErreur(null)
    setCode('')
    setSecretVisible(false)
    try {
      setEnrolement(await mutateAsync())
    } catch (x) {
      setEnrolement(null)
      setErreur({ ...messageRef.current(x), regenerer: true })
    }
  }, [mutateAsync])

  useEffect(() => {
    if (lance.current) return
    lance.current = true
    void nouveau()
  }, [nouveau])

  const soumettre = async () => {
    setErreur(null)
    if (!enrolement) return
    setChamp(CODE.test(code) ? undefined : t('connexion.code.format'))
    if (!CODE.test(code)) return
    try {
      await confirmer.mutateAsync({ enrolementId: enrolement.enrolementId, code, ...(libelle.trim() ? { libelle: libelle.trim() } : {}) })
      setEnrolement(null)
      onFait()
    } catch (x) {
      const m = message(x)
      setCode('')
      if (m.code === 'IAM_TOTP_CODE_INVALID') setChamp(m.texte)
      else setErreur({ ...m, regenerer: m.code === 'IAM_TOTP_ENROLMENT_EXPIRED' })
    }
  }

  const contenu = (
    <>
      {remplacement ? <InlineAlert level="warn" title={t('totp.remplacement')} /> : null}
      <ol className="gm-col" style={{ margin: 0, paddingInlineStart: 'var(--gm-space-5)', gap: 'var(--gm-space-2)' }}>
        <li>{t('totp.etape1')}</li>
        <li>{t('totp.etape2')}</li>
        <li>{t('totp.etape3')}</li>
      </ol>
      {erreur ? (
        <InlineAlert level="crit" title={erreur.texte} correlationId={erreur.correlationId}
          action={erreur.regenerer ? <Button size="sm" icon="refresh" onClick={() => void nouveau()}>{t('totp.regenerer')}</Button> : undefined} />
      ) : null}
      {enroler.isPending ? <Skeleton height={192} width={192} /> : null}
      {enrolement ? (
        <>
          <QrCode value={enrolement.uri} label={t('totp.qr')} data-testid="totp-qr" />
          <div className="gm-row" style={{ alignItems: 'center' }}>
            <Button size="sm" variant="ghost" icon="eye" onClick={() => setSecretVisible(!secretVisible)}>
              {t(secretVisible ? 'totp.secret.masquer' : 'totp.secret.afficher')}
            </Button>
            {secretVisible ? <span className="gm-chip gm-mono" data-testid="totp-secret" aria-label={t('totp.secret')}>{grouper(enrolement.secret)}</span> : null}
          </div>
          <div style={{ maxWidth: 320 }}>
            <TextField name="codeTotp" label={t('totp.code')} value={code} onChange={(v) => setCode(v.replace(/\D/g, '').slice(0, 6))} required mono
              inputMode="numeric" maxLength={6} autoComplete="one-time-code" help={t('connexion.code.aide')} error={champ} />
          </div>
          <div style={{ maxWidth: 420 }}>
            <TextField name="libelleTotp" label={t('totp.libelle')} value={libelle} onChange={setLibelle} maxLength={60} help={t('compte.facultatif')} />
          </div>
        </>
      ) : null}
    </>
  )
  return { contenu, soumettre, enCours: confirmer.isPending, pret: !!enrolement, modifie: !!(code || libelle) }
}
