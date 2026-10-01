import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, Checkbox, CrudDialog, InlineAlert, NumberField, PageHeader, Select, SettingsGrid } from '@gimac/design-system'
import { aPermission, estApiError, usePermissions } from '@gimac/socle'
import { useEffet, usePolitique } from '../hooks'
import { ALGORITHMES_OTP, CHIFFRES_OTP, PLANCHERS, type PolitiqueSecurite } from '../modele/configuration'
import { Annonce } from '../composants/Annonce'
import { useActes } from '../composants/actes'
import { Donnees } from '../composants/communs'

// Politique de sécurité de l'authentification (D-62) : mots de passe, second facteur, sessions,
// protection contre la force brute. Lecture avec PLATFORM_SECURITY_POLICY_READ ; modification dans une
// fenêtre modale avec PLATFORM_SECURITY_POLICY_EDIT, puis motif et réauthentification FIDO2. Les planchers sont rappelés en aide ;
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

const versPolitique = (v: Saisie, t: (k: string) => string): { p: PolitiqueSecurite; erreurs: Record<string, string> } => {
  const erreurs: Record<string, string> = {}
  const p = Object.fromEntries(
    Object.entries(v).map(([k, x]) => {
      if (typeof x === 'boolean' || k === 'otpAlgorithme') return [k, x]
      const n = Number(String(x).trim())
      if (String(x).trim() === '' || !Number.isInteger(n)) erreurs[k] = t('console.politique.entier')
      return [k, n]
    }),
  ) as PolitiqueSecurite
  return { p, erreurs }
}

/** Modification de la politique dans une fenêtre modale ; l'application passe ensuite par la signature FIDO2. */
function DialoguePolitique({ initiale, erreursInitiales, onFermer, onValider }: {
  initiale: Saisie
  erreursInitiales: Record<string, string>
  onFermer: () => void
  onValider: (p: PolitiqueSecurite, saisie: Saisie) => void
}) {
  const { t } = useTranslation()
  const [v, setV] = useState<Saisie>(initiale)
  const [erreurs, setErreurs] = useState<Record<string, string>>(erreursInitiales)
  const champ = (k: keyof Saisie) => (x: string | boolean) => setV((o) => ({ ...o, [k]: x }))

  const soumettre = () => {
    const r = versPolitique(v, t)
    setErreurs(r.erreurs)
    if (Object.keys(r.erreurs).length) return
    onValider(r.p, v)
  }

  const rendre = (k: keyof PolitiqueSecurite) => {
    const aide = PLANCHERS[k] ? t('console.politique.plancher', { min: PLANCHERS[k]![0], max: PLANCHERS[k]![1] }) : undefined
    if (k === 'interditIdentifiant' || k === 'forceBruteActive') {
      return (
        <div key={k} className="gm-form-field gm-form-wide">
          <Checkbox name={k} label={t(`console.politique.${k}`)} checked={v[k]} onChange={champ(k)} error={erreurs[k]} />
          {k === 'forceBruteActive' ? <span className="gm-field-help">{t('console.politique.forceBruteActive.aide')}</span> : null}
        </div>
      )
    }
    if (k === 'otpChiffres' || k === 'otpAlgorithme') {
      const options = k === 'otpChiffres' ? CHIFFRES_OTP.map((n) => ({ value: String(n), label: String(n) })) : ALGORITHMES_OTP.map((a) => ({ value: a, label: a }))
      return (
        <div key={k} className="gm-form-field">
          <Select name={k} label={t(`console.politique.${k}`)} value={v[k]} onChange={champ(k)} options={options} error={erreurs[k]}
            help={t(k === 'otpChiffres' ? 'console.politique.otpChiffres.aide' : 'console.politique.otpAlgorithme.aide')} />
        </div>
      )
    }
    return (
      <div key={k} className="gm-form-field">
        <NumberField name={k} label={t(`console.politique.${k}`)} value={v[k]} onChange={champ(k)} error={erreurs[k]} help={aide} unit={UNITES[k] ? t(UNITES[k]!) : undefined} />
      </div>
    )
  }

  return (
    <CrudDialog context="edit" id="gm-dlg-politique" title={t('console.politique.modifier')} subject={t('console.politique.titre')} description={t('console.politique.modifier.texte')} onClose={onFermer} onSubmit={soumettre}
      submitLabel={t('console.politique.appliquer')} submitIcon="pen-line" reauth dirty={JSON.stringify(v) !== JSON.stringify(initiale)}>
      {GROUPES.map((g) => (
        <fieldset key={g.cle} className="gm-col" style={{ border: 0, margin: 0, padding: 0 }}>
          <legend className="gm-type-h3" style={{ padding: 0, marginBottom: 'var(--gm-space-3)' }}>{t(`console.politique.groupe.${g.cle}`)}</legend>
          <div className="gm-form-grid">{g.champs.map(rendre)}</div>
        </fieldset>
      ))}
    </CrudDialog>
  )
}


/** Politique en vigueur, en lecture : une carte par groupe de règles, chaque valeur mise en valeur avec son plancher. */
function Lecture({ politique }: { politique: PolitiqueSecurite }) {
  const { t } = useTranslation()
  const item = (k: keyof PolitiqueSecurite) => {
    const x = politique[k]
    const plancher = PLANCHERS[k] ? t('console.politique.plancher', { min: PLANCHERS[k]![0], max: PLANCHERS[k]![1] }) : undefined
    if (typeof x === 'boolean') return { key: k, label: t(`console.politique.${k}`), value: t(x ? 'console.politique.active' : 'console.politique.inactive'), status: x ? ('ok' as const) : ('neutral' as const), hint: plancher }
    return { key: k, label: t(`console.politique.${k}`), value: String(x), unit: UNITES[k] ? t(UNITES[k]!) : undefined, hint: plancher }
  }
  return (
    <div className="gm-grille-cartes">
      {GROUPES.map((g) => (
        <Card key={g.cle} title={t(`console.politique.groupe.${g.cle}`)}>
          <SettingsGrid label={t(`console.politique.groupe.${g.cle}`)} items={g.champs.map(item)} />
        </Card>
      ))}
    </div>
  )
}

export default function PolitiqueSecuritePage() {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const politique = usePolitique()
  const appliquer = useEffet((s, v: { p: PolitiqueSecurite; motif: string; assertion: string }) => s.configuration.appliquerPolitique(v.p, v.motif, v.assertion))
  // Actes et annonce au niveau de la page ; la saisie et les refus du service rouvrent la modale.
  const actes = useActes()
  const [edition, setEdition] = useState<{ saisie: Saisie; erreurs: Record<string, string> } | null>(null)
  const modifiable = aPermission(permissions, 'PLATFORM_SECURITY_POLICY_EDIT')

  const valider = (p: PolitiqueSecurite, saisie: Saisie) => {
    setEdition(null)
    actes.sensible({
      titre: t('console.politique.appliquer'),
      contenu: p,
      succes: t('console.politique.appliquee'),
      executer: (motif, assertion) => appliquer.mutateAsync({ p, motif, assertion }),
      surErreur: (x) => {
        if (!estApiError(x) || x.code !== 'IAM_POLICY_INVALID') return false
        const champs = ((x.details?.champs ?? {}) as Record<string, string>)
        const erreurs = Object.fromEntries(Object.entries(champs).map(([k, regle]) => [k === 'classes' ? 'majusculesMin' : k, k === 'classes' ? t('console.politique.refus.classes') : t('console.politique.refus', { regle })]))
        actes.setRetour({ niveau: 'crit', message: { texte: t('erreur.IAM_POLICY_INVALID'), correlationId: x.correlationId || undefined } })
        setEdition({ saisie, erreurs })
        return true
      },
    })
  }

  return (
    <div className="gm-col">
      <PageHeader id="gm-politique-titre" title={t('console.politique.titre')} subtitle={t('console.politique.texte')}
        actions={modifiable && politique.data ? <Button variant="primary" icon="pen-line" onClick={() => { actes.setRetour(null); setEdition({ saisie: versSaisie(politique.data!), erreurs: {} }) }}>{t('console.politique.modifier')}</Button> : undefined} />
      {!modifiable ? <InlineAlert level="info" title={t('console.politique.lectureSeule')} /> : null}
      <Annonce retour={actes.retour} />
      <Donnees requete={politique}>{(p) => <Lecture politique={p} />}</Donnees>
      {edition ? <DialoguePolitique initiale={edition.saisie} erreursInitiales={edition.erreurs} onFermer={() => setEdition(null)} onValider={valider} /> : null}
      {actes.dialogue}
    </div>
  )
}
