import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button, CheckboxGroup, CrudDialog, InlineAlert, KeyValueList, OneTimeSecret, RadioGroup, TextField } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { useLibelleProfil, useMessageErreur, type MessageErreur } from '@gimac/compte'
import { useEffet, useProfils } from '../hooks'
import type { Creation, NouveauCompte } from '../modele/comptes'
import { Donnees } from './communs'
import { useChemins } from '../services/useServicesConsole'

// Création d'un compte (D-62) par le super administrateur ou le RSSI, dans une fenêtre modale ouverte
// depuis la liste des comptes ou le tableau de bord : aucune inscription libre. Le titulaire complète son
// compte à son premier accès. Invitation par courriel ou mot de passe provisoire, affiché une seule fois
// dans le dialogue de résultat. Un profil privilégié n'est proposé qu'avec IAM_ROLE_PRIVILEGED_GRANT.

type Accueil = 'INVITATION' | 'MOT_DE_PASSE' | 'LES_DEUX'
type Valeurs = Omit<NouveauCompte, 'telephone' | 'fonction' | 'institutionId' | 'envoyerInvitation' | 'motDePasseProvisoire'> & {
  telephone: string
  fonction: string
  institutionId: string
  accueil: Accueil
}
const VIDE: Valeurs = { identifiant: '', prenom: '', nom: '', email: '', telephone: '', fonction: '', institutionId: '', profils: [], accueil: 'INVITATION' }
const CHAMP_DU_CODE: Record<string, string> = {
  IAM_USERNAME_INVALID: 'identifiant', IAM_PHONE_INVALID: 'telephone', IAM_EMAIL_REQUIRED: 'email', IAM_ACCOUNT_CONFLICT: 'identifiant',
  IAM_ONBOARDING_REQUIRED: 'accueil', IAM_PROFILE_UNKNOWN: 'profils', IAM_PRIVILEGED_PROFILE: 'profils',
  IAM_INSTITUTION_INTERNAL_ACCOUNT: 'institutionId', IAM_INSTITUTION_REQUIRED: 'institutionId',
}

function Resultat({ creation, onAutre, onFermer }: { creation: Creation; onAutre: () => void; onFermer: () => void }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { base } = useChemins()
  return (
    <CrudDialog context="detail" id="gm-dlg-creation-resultat" title={t('console.creation.cree', { identifiant: creation.compte.identifiant })} subject={creation.compte.email ?? undefined} onClose={onFermer}
      extraActions={<>
        <Button onClick={onAutre}>{t('console.creation.autre')}</Button>
        <Button variant="primary" onClick={() => void navigate(`${base}/comptes/${creation.compte.id}`)}>{t('console.creation.ouvrir')}</Button>
      </>}>
      <div className="gm-col">
        <KeyValueList
          items={[
            { key: t('compte.identifiant'), value: creation.compte.identifiant, mono: true },
            { key: t('compte.email'), value: creation.compte.email ?? '—' },
            { key: t('console.creation.invitation'), value: t(creation.invitationEnvoyee ? 'console.creation.invitation.envoyee' : 'console.creation.invitation.non') },
          ]}
        />
        {creation.motDePasseProvisoire ? (
          <OneTimeSecret label={t('console.creation.mdp.titre')} value={creation.motDePasseProvisoire} warning={t('console.creation.mdp.avertissement')} />
        ) : null}
      </div>
    </CrudDialog>
  )
}

export function DialogueCreationCompte({ onFermer }: { onFermer: () => void }) {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const message = useMessageErreur()
  const libelleProfil = useLibelleProfil()
  const profils = useProfils()
  const creer = useEffet((s, n: NouveauCompte) => s.comptes.creer(n))
  const [v, setV] = useState<Valeurs>(VIDE)
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  const [erreur, setErreur] = useState<MessageErreur | null>(null)
  const [creation, setCreation] = useState<Creation | null>(null)
  const privilegie = aPermission(permissions, 'IAM_ROLE_PRIVILEGED_GRANT')
  const champ = (k: keyof Valeurs) => (x: unknown) => setV((o) => ({ ...o, [k]: x }))

  if (creation) return <Resultat creation={creation} onFermer={onFermer} onAutre={() => { setCreation(null); setV(VIDE) }} />

  const soumettre = async () => {
    const requis = t('console.creation.requis')
    const locales: Record<string, string> = {}
    ;(['identifiant', 'prenom', 'nom', 'email'] as const).forEach((k) => {
      if (!v[k].trim()) locales[k] = requis
    })
    if (!v.profils.length) locales.profils = t('console.creation.profils.requis')
    setErreurs(locales)
    setErreur(null)
    if (Object.keys(locales).length) return
    try {
      setCreation(
        await creer.mutateAsync({
          identifiant: v.identifiant.trim().toLowerCase(), prenom: v.prenom.trim(), nom: v.nom.trim(), email: v.email.trim(), profils: v.profils,
          telephone: v.telephone.trim() || null, fonction: v.fonction.trim() || null, institutionId: v.institutionId.trim() || null,
          envoyerInvitation: v.accueil !== 'MOT_DE_PASSE', motDePasseProvisoire: v.accueil !== 'INVITATION',
        }),
      )
    } catch (x) {
      const m = message(x)
      const cible = m.code ? CHAMP_DU_CODE[m.code] : undefined
      if (cible) setErreurs({ [cible]: m.texte })
      else setErreur(m)
    }
  }

  return (
    <CrudDialog context="create" dirty={JSON.stringify(v) !== JSON.stringify(VIDE)} id="gm-dlg-creation" title={t('console.creation.titre')} description={t('console.creation.texte')} onClose={onFermer} onSubmit={() => void soumettre()}
      submitLabel={t('console.creation.creer')} submitIcon="plus" submitting={creer.isPending}
      error={erreur ? { title: erreur.texte, correlationId: erreur.correlationId } : undefined}>
      <div className="gm-form-grid">
        <div className="gm-form-field"><TextField name="identifiant" label={t('compte.identifiant')} required mono value={v.identifiant} onChange={champ('identifiant')} error={erreurs.identifiant} help={t('console.creation.identifiant.aide')} /></div>
        <div className="gm-form-field"><TextField name="email" type="email" label={t('compte.email')} required value={v.email} onChange={champ('email')} error={erreurs.email} /></div>
        <div className="gm-form-field"><TextField name="prenom" label={t('compte.prenom')} required value={v.prenom} onChange={champ('prenom')} error={erreurs.prenom} /></div>
        <div className="gm-form-field"><TextField name="nom" label={t('compte.nom')} required value={v.nom} onChange={champ('nom')} error={erreurs.nom} /></div>
        <div className="gm-form-field"><TextField name="telephone" type="tel" label={t('compte.telephone')} value={v.telephone} onChange={champ('telephone')} error={erreurs.telephone} help={t('compte.telephone.aide')} /></div>
        <div className="gm-form-field"><TextField name="fonction" label={t('compte.fonction')} value={v.fonction} onChange={champ('fonction')} /></div>
        <div className="gm-form-field"><TextField name="institutionId" label={t('console.creation.institution')} mono value={v.institutionId} onChange={champ('institutionId')} help={t('console.creation.institution.aide')} /></div>
        <div className="gm-form-field gm-form-wide">
          <Donnees requete={profils}>
            {(l) => (
              <CheckboxGroup
                id="profils"
                label={t('console.creation.profils')}
                value={v.profils}
                onChange={champ('profils')}
                error={erreurs.profils}
                options={l.filter((p) => privilegie || !p.privilegie).map((p) => ({ value: p.code, label: libelleProfil(p.code) }))}
              />
            )}
          </Donnees>
        </div>
        <div className="gm-form-field gm-form-wide">
          <RadioGroup name="accueil" label={t('console.creation.accueil')} value={v.accueil} onChange={champ('accueil')}
            options={(['INVITATION', 'MOT_DE_PASSE', 'LES_DEUX'] as const).map((a) => ({ value: a, label: t(`console.creation.accueil.${a}`) }))} />
          {erreurs.accueil ? <InlineAlert level="crit" title={erreurs.accueil} /> : null}
        </div>
      </div>
    </CrudDialog>
  )
}
