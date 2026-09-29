import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, Checkbox, InlineAlert, NumberField, Select } from '@gimac/design-system'
import { aPermission, estApiError, usePermissions } from '@gimac/socle'
import { useEffet, usePolitique } from '../hooks'
import { ALGORITHMES_OTP, CHIFFRES_OTP, PLANCHERS, type PolitiqueSecurite } from '../modele/configuration'
import { Annonce } from '../composants/Annonce'
import { useActes } from '../composants/actes'
import { Donnees } from '../composants/communs'

// Politique de sécurité de l'authentification (D-62) : mots de passe, second facteur, sessions,
// protection contre la force brute. Lecture avec PLATFORM_SECURITY_POLICY_READ ; application avec
// PLATFORM_SECURITY_POLICY_EDIT, motif et réauthentification FIDO2. Les planchers sont rappelés en aide ;
// MS-IAM les impose et renvoie IAM_POLICY_INVALID avec l'erreur de chaque champ (details.champs).

type Numerique = Exclude<keyof PolitiqueSecurite, 'interditIdentifiant' | 'forceBruteActive' | 'otpAlgorithme' | 'otpChiffres'>
type Saisie = Record<Numerique, string> & { otpChiffres: string; otpAlgorithme: string; interditIdentifiant: boolean; forceBruteActive: boolean }

const GROUPES: { cle: string; champs: (keyof PolitiqueSecurite)[] }[] = [
  { cle: 'motsDePasse', champs: ['longueurMinimale', 'majusculesMin', 'minusculesMin', 'chiffresMin', 'speciauxMin', 'historique', 'expirationJours', 'interditIdentifiant'] },
  { cle: 'secondFacteur', champs: ['otpChiffres', 'otpPeriodeSecondes', 'otpAlgorithme'] },
  { cle: 'sessions', champs: ['sessionInactiviteMinutes', 'sessionDureeMaxHeures'] },
  { cle: 'forceBrute', champs: ['forceBruteActive', 'forceBruteEchecsMax', 'forceBruteAttenteMinutes'] },
]
const UNITES: Partial<Record<keyof PolitiqueSecurite, string>> = {
  expirationJours: 'console.unite.jours', otpPeriodeSecondes: 'console.unite.secondes', sessionInactiviteMinutes: 'console.unite.minutes',
  sessionDureeMaxHeures: 'console.unite.heures', forceBruteAttenteMinutes: 'console.unite.minutes',
}

const versSaisie = (p: PolitiqueSecurite): Saisie =>
  Object.fromEntries(Object.entries(p).map(([k, v]) => [k, typeof v === 'number' ? String(v) : v])) as Saisie

function Formulaire({ politique, modifiable, actes }: { politique: PolitiqueSecurite; modifiable: boolean; actes: ReturnType<typeof useActes> }) {
  const { t } = useTranslation()
  const appliquer = useEffet((s, v: { p: PolitiqueSecurite; motif: string; assertion: string }) => s.configuration.appliquerPolitique(v.p, v.motif, v.assertion))
  const [v, setV] = useState<Saisie>(() => versSaisie(politique))
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  const champ = (k: keyof Saisie) => (x: string | boolean) => setV((o) => ({ ...o, [k]: x }))

  const soumettre = (e: FormEvent) => {
    e.preventDefault()
    const locales: Record<string, string> = {}
    const p = Object.fromEntries(
      Object.entries(v).map(([k, x]) => {
        if (typeof x === 'boolean' || k === 'otpAlgorithme') return [k, x]
        const n = Number(String(x).trim())
        if (String(x).trim() === '' || !Number.isInteger(n)) locales[k] = t('console.politique.entier')
        return [k, n]
      }),
    ) as PolitiqueSecurite
    setErreurs(locales)
    if (Object.keys(locales).length) return
    actes.sensible({
      titre: t('console.politique.appliquer'),
      contenu: p,
      succes: t('console.politique.appliquee'),
      executer: (motif, assertion) => appliquer.mutateAsync({ p, motif, assertion }),
      surErreur: (x) => {
        if (!estApiError(x) || x.code !== 'IAM_POLICY_INVALID') return false
        const champs = ((x.details?.champs ?? {}) as Record<string, string>)
        setErreurs(Object.fromEntries(Object.entries(champs).map(([k, regle]) => [k === 'classes' ? 'majusculesMin' : k, k === 'classes' ? t('console.politique.refus.classes') : t('console.politique.refus', { regle })])))
        actes.setRetour({ niveau: 'crit', message: { texte: t('erreur.IAM_POLICY_INVALID'), correlationId: x.correlationId || undefined } })
        return true
      },
    })
  }

  const rendre = (k: keyof PolitiqueSecurite) => {
    const aide = PLANCHERS[k] ? t('console.politique.plancher', { min: PLANCHERS[k]![0], max: PLANCHERS[k]![1] }) : undefined
    if (k === 'interditIdentifiant' || k === 'forceBruteActive') {
      return (
        <div key={k} className="gm-form-field gm-form-wide">
          <Checkbox name={k} label={t(`console.politique.${k}`)} checked={v[k]} disabled={!modifiable} onChange={champ(k)} error={erreurs[k]} />
          {k === 'forceBruteActive' ? <span className="gm-field-help">{t('console.politique.forceBruteActive.aide')}</span> : null}
        </div>
      )
    }
    if (k === 'otpChiffres' || k === 'otpAlgorithme') {
      const options = k === 'otpChiffres' ? CHIFFRES_OTP.map((n) => ({ value: String(n), label: String(n) })) : ALGORITHMES_OTP.map((a) => ({ value: a, label: a }))
      return (
        <div key={k} className="gm-form-field">
          <Select name={k} label={t(`console.politique.${k}`)} value={v[k]} onChange={champ(k)} options={options} disabled={!modifiable} error={erreurs[k]}
            help={t(k === 'otpChiffres' ? 'console.politique.otpChiffres.aide' : 'console.politique.otpAlgorithme.aide')} />
        </div>
      )
    }
    return (
      <div key={k} className="gm-form-field">
        <NumberField name={k} label={t(`console.politique.${k}`)} value={v[k]} onChange={champ(k)} readOnly={!modifiable} error={erreurs[k]} help={aide}
          unit={UNITES[k] ? t(UNITES[k]!) : undefined} />
      </div>
    )
  }

  return (
    <form className="gm-col" onSubmit={soumettre} aria-labelledby="gm-politique-titre" noValidate>
      {!modifiable ? <InlineAlert level="info" title={t('console.politique.lectureSeule')} /> : null}
      {GROUPES.map((g) => (
        <Card key={g.cle} title={t(`console.politique.groupe.${g.cle}`)}>
          <div className="gm-form-grid">{g.champs.map(rendre)}</div>
        </Card>
      ))}
      {modifiable ? (
        <div><Button variant="primary" type="submit" icon="pen-line">{t('console.politique.appliquer')}</Button></div>
      ) : null}
    </form>
  )
}

export default function PolitiqueSecuritePage() {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const politique = usePolitique()
  // Actes et annonce au niveau de la page : le formulaire est remonté après chaque relecture.
  const actes = useActes()
  return (
    <div className="gm-col">
      <h1 id="gm-politique-titre" className="gm-heading-display">{t('console.politique.titre')}</h1>
      <p style={{ margin: 0 }}>{t('console.politique.texte')}</p>
      <Annonce retour={actes.retour} />
      <Donnees requete={politique}>{(p) => <Formulaire key={JSON.stringify(p)} politique={p} actes={actes} modifiable={aPermission(permissions, 'PLATFORM_SECURITY_POLICY_EDIT')} />}</Donnees>
      {actes.dialogue}
    </div>
  )
}
