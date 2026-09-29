import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Button, Card, CheckboxGroup, InlineAlert, KeyValueList, OneTimeSecret, RadioGroup, TextField } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { useLibelleProfil, useMessageErreur, type MessageErreur } from '@gimac/compte'
import { useEffet, useProfils } from '../hooks'
import type { Creation, NouveauCompte } from '../modele/comptes'
import { Donnees } from '../composants/communs'

// Création d'un compte (D-62) par le super administrateur ou le RSSI : aucune inscription libre. Le
// titulaire complète son compte à son premier accès. Invitation par courriel ou mot de passe provisoire,
// affiché une seule fois. Un profil privilégié n'est proposé qu'avec IAM_ROLE_PRIVILEGED_GRANT.

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
}

function Resultat({ creation, onAutre }: { creation: Creation; onAutre: () => void }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  return (
    <Card title={t('console.creation.cree', { identifiant: creation.compte.identifiant })}>
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
        <div className="gm-row">
          <Button variant="primary" onClick={() => void navigate(`/console/comptes/${creation.compte.id}`)}>{t('console.creation.ouvrir')}</Button>
          <Button onClick={onAutre}>{t('console.creation.autre')}</Button>
        </div>
      </div>
    </Card>
  )
}

export default function CreationComptePage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
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

  if (creation) return <Resultat creation={creation} onAutre={() => { setCreation(null); setV(VIDE) }} />

  const soumettre = async (e: FormEvent) => {
    e.preventDefault()
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
    <form className="gm-resource gm-form" onSubmit={(e) => void soumettre(e)} aria-labelledby="gm-creation-titre" noValidate>
      <h1 id="gm-creation-titre" className="gm-heading-display">{t('console.creation.titre')}</h1>
      <p style={{ margin: 0 }}>{t('console.creation.texte')}</p>
      {erreur ? <InlineAlert level="crit" title={erreur.texte} correlationId={erreur.correlationId} /> : null}
      <Card>
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
      </Card>
      <div className="gm-form-actions">
        <Button variant="ghost" onClick={() => void navigate('/console/comptes')}>{t('commun.annuler')}</Button>
        <span style={{ flex: 1 }} />
        <Button variant="primary" type="submit" icon="plus" loading={creer.isPending}>{t('console.creation.creer')}</Button>
      </div>
    </form>
  )
}
