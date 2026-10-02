import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { ConfirmDialog, InlineAlert, Link, SignDialog, type KeyValueItem } from '@gimac/design-system'
import { useMessageErreur, type MessageErreur } from '@gimac/compte'
import { useChemins, useServicesConsole } from '../services/useServicesConsole'

// Exécution des actes de la console, toujours composés du design system :
// - acte motivé : ConfirmDialog avec motif obligatoire (activation, sécurité du compte…) ;
// - acte sensible : SignDialog avec motif et réauthentification FIDO2 ADMIN_SENSITIVE (profils,
//   suppression, politique de sécurité, composition d'un profil). 428 : réauthentification exigée ;
//   403 IAM_REAUTH_INVALID : refusée ; 409 IAM_NO_SECURITY_KEY : lien vers « Mon compte ».
// Le résultat est annoncé dans une zone aria-live de la page.

export type Retour = { niveau: 'info' | 'warn' | 'crit'; message: MessageErreur } | null

type Commun = {
  titre: string
  objet?: KeyValueItem[]
  succes: string
  /** Erreur traitée par la page (erreurs de champ) : le dialogue se ferme sans message. */
  surErreur?: (e: unknown) => boolean
  extra?: ReactNode
}
export type ActeMotive = Commun & { description?: string; libelle: string; danger?: boolean; irreversible?: boolean; executer: (motif: string) => Promise<unknown> }
export type ActeSensible = Commun & { contenu: unknown; executer: (motif: string, assertion: string) => Promise<unknown> }

/** Empreinte SHA-256 (hexadécimal) du contenu de l'acte, affichée dans le dialogue de signature. */
export async function empreinte(contenu: unknown): Promise<string> {
  const octets = new TextEncoder().encode(JSON.stringify(contenu ?? null))
  const d = await crypto.subtle.digest('SHA-256', octets)
  return Array.from(new Uint8Array(d), (b) => b.toString(16).padStart(2, '0')).join('')
}

export function useActes() {
  const { espace } = useChemins()
  const { t } = useTranslation()
  const services = useServicesConsole()
  const message = useMessageErreur()
  const [motive, setMotive] = useState<ActeMotive | null>(null)
  const [sensible, setSensible] = useState<ActeSensible | null>(null)
  const [hash, setHash] = useState('')
  const [etat, setEtat] = useState<'idle' | 'waiting' | 'done' | 'error'>('idle')
  const [erreur, setErreur] = useState<MessageErreur | null>(null)
  const [retour, setRetour] = useState<Retour>(null)

  useEffect(() => {
    if (!sensible) return
    let actif = true
    void empreinte(sensible.contenu).then((h) => actif && setHash(h))
    return () => {
      actif = false
    }
  }, [sensible])

  const fermer = useCallback(() => {
    setSensible(null)
    setMotive(null)
    setEtat('idle')
    setErreur(null)
    setHash('')
  }, [])

  const confirmerMotive = async (motif: string) => {
    const a = motive!
    setMotive(null)
    try {
      await a.executer(motif)
      setRetour({ niveau: 'info', message: { texte: a.succes } })
    } catch (e) {
      if (a.surErreur?.(e)) return
      setRetour({ niveau: 'crit', message: message(e) })
    }
  }

  const signer = async (motif: string) => {
    const a = sensible!
    setEtat('waiting')
    setErreur(null)
    try {
      const assertion = await services.reauth.assertion('ADMIN_SENSITIVE')
      await a.executer(motif, assertion)
      setEtat('done')
      setRetour({ niveau: 'info', message: { texte: a.succes } })
    } catch (e) {
      if (a.surErreur?.(e)) {
        fermer()
        return
      }
      setEtat('error')
      setErreur(message(e))
    }
  }

  const dialogue = motive ? (
    <ConfirmDialog
      open
      title={motive.titre}
      description={motive.description}
      confirmLabel={motive.libelle}
      danger={motive.danger}
      irreversible={motive.irreversible}
      reason
      object={motive.objet}
      extra={motive.extra}
      onConfirm={(m) => void confirmerMotive(m)}
      onClose={fermer}
    />
  ) : sensible ? (
    <SignDialog
      open
      title={sensible.titre}
      action={sensible.titre}
      hash={hash}
      state={etat}
      reason
      object={sensible.objet}
      extra={
        <>
          {sensible.extra}
          {erreur?.code === 'SANS_CLE' ? <Link href={`${espace}/mon-compte`}>{t('reauth.versMonCompte')}</Link> : null}
          {erreur?.correlationId ? <InlineAlert level="info" correlationId={erreur.correlationId} /> : null}
        </>
      }
      error={erreur?.texte}
      onSign={(m) => void signer(m)}
      onClose={fermer}
      onDone={fermer}
    />
  ) : null

  return {
    /** Acte avec motif obligatoire (ConfirmDialog). */
    motive: (a: ActeMotive) => {
      setRetour(null)
      setMotive(a)
    },
    /** Acte sensible : motif et réauthentification FIDO2 (SignDialog). */
    sensible: (a: ActeSensible) => {
      setRetour(null)
      setEtat('idle')
      setErreur(null)
      setSensible(a)
    },
    dialogue,
    retour,
    setRetour,
  }
}
