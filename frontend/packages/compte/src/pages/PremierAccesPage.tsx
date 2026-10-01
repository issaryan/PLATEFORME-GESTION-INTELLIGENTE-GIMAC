import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, Checkbox, InlineAlert, Panel, Select, Stepper, TextField, PageHeader } from '@gimac/design-system'
import { estApiError, navigationDocument, retourCourant, useConnexion, usePreferences, useSession } from '@gimac/socle'
import { etapesAssistant, LANGUES_COMPTE, type EtapePremierAcces, type LangueCompte, type Moi } from '../modele'
import { usePremiereConfiguration } from '../hooks'
import { useMessageErreur, type MessageErreur } from '../outils'
import { useSaisieMotDePasse } from '../composants/saisieMotDePasse'
import { useSaisieTotp } from '../composants/saisieTotp'

// Assistant de premier accès (D-62, D-65) : un compte créé par le super administrateur ou le RSSI est
// complété par son titulaire avant tout tableau de bord, entièrement sur la plateforme. Étapes : mot de
// passe définitif (politique rappelée), code TOTP (code QR calculé localement, secret affichable, code de
// vérification), fiche, charte ; puis, dans la session restreinte du premier accès, connexion avec le
// nouveau mot de passe et un code (POST /api/connexion), qui ouvre la session complète.

type Etape = EtapePremierAcces | 'CONNEXION'
type Fiche = { telephone: string; langue: LangueCompte; fonction: string | null }

const TELEPHONE = /^\+?[0-9 ]{8,20}$/
const CODE = /^[0-9]{6}$/

const langueValide = (l: string | null | undefined): LangueCompte | undefined =>
  l && (LANGUES_COMPTE as readonly string[]).includes(l) ? (l as LangueCompte) : undefined

function Titre({ id, cle }: { id: string; cle: string }) {
  const { t } = useTranslation()
  return <h2 id={id} className="gm-type-h3" style={{ margin: 0 }}>{t(cle)}</h2>
}

function EtapeMotDePasse({ onFait }: { onFait: (nouveau: string) => void }) {
  const { t } = useTranslation()
  const saisie = useSaisieMotDePasse({ provisoire: true, onFait })
  return (
    <form className="gm-col" onSubmit={(e) => { e.preventDefault(); void saisie.soumettre() }} aria-labelledby="gm-premier-mdp" noValidate>
      <Titre id="gm-premier-mdp" cle="premierAcces.motDePasse.titre" />
      <p className="gm-section-desc">{t('premierAcces.motDePasse.texte')}</p>
      {saisie.erreur ? <InlineAlert level="crit" title={saisie.erreur.title} correlationId={saisie.erreur.correlationId}>{saisie.erreur.text}</InlineAlert> : null}
      {saisie.contenu}
      <div>
        <Button variant="primary" type="submit" icon="key-round" loading={saisie.enCours}>{t('motDePasse.enregistrer')}</Button>
      </div>
    </form>
  )
}

function EtapeTotp({ onFait }: { onFait: () => void }) {
  const { t } = useTranslation()
  const saisie = useSaisieTotp({ onFait })
  return (
    <form className="gm-col" onSubmit={(e) => { e.preventDefault(); void saisie.soumettre() }} aria-labelledby="gm-premier-totp" noValidate>
      <Titre id="gm-premier-totp" cle="premierAcces.totp.titre" />
      <p className="gm-section-desc">{t('premierAcces.totp.texte')}</p>
      {saisie.contenu}
      <div>
        <Button variant="primary" type="submit" icon="shield-check" disabled={!saisie.pret} disabledReason={t('totp.attente')} loading={saisie.enCours}>{t('totp.activer')}</Button>
      </div>
    </form>
  )
}

function EtapeFiche({ moi, charteEnSuite, onFiche }: { moi: Moi; charteEnSuite: boolean; onFiche: (f: Fiche) => Promise<boolean> | boolean }) {
  const { t, i18n } = useTranslation()
  const [telephone, setTelephone] = useState(moi.compte.telephone ?? '')
  const [langue, setLangue] = useState<LangueCompte>(langueValide(moi.compte.langue) ?? langueValide(i18n.language.slice(0, 2)) ?? 'fr')
  const [fonction, setFonction] = useState(moi.compte.fonction ?? '')
  const [erreur, setErreur] = useState<string | undefined>()
  const [enCours, setEnCours] = useState(false)
  const soumettre = async (e: FormEvent) => {
    e.preventDefault()
    const tel = telephone.trim()
    const m = !tel ? t('premierAcces.fiche.telephone.requis') : TELEPHONE.test(tel) ? undefined : t('erreur.IAM_PHONE_INVALID')
    setErreur(m)
    if (m) return
    setEnCours(true)
    try {
      await onFiche({ telephone: tel, langue, fonction: fonction.trim() || null })
    } finally {
      setEnCours(false)
    }
  }
  return (
    <form className="gm-col" onSubmit={(e) => void soumettre(e)} aria-labelledby="gm-premier-fiche" noValidate>
      <Titre id="gm-premier-fiche" cle="premierAcces.fiche.titre" />
      <p className="gm-section-desc">{t('premierAcces.fiche.texte')}</p>
      <div className="gm-form-grid">
        <div className="gm-form-field">
          <TextField name="telephone" type="tel" label={t('compte.telephone')} required value={telephone} onChange={setTelephone} error={erreur} help={t('compte.telephone.aide')} autoComplete="tel" />
        </div>
        <div className="gm-form-field">
          <Select name="langue" label={t('compte.langue')} required value={langue} onChange={(v) => setLangue(v as LangueCompte)}
            options={LANGUES_COMPTE.map((l) => ({ value: l, label: t(`compte.langue.${l}`) }))} />
        </div>
        <div className="gm-form-field gm-form-wide">
          <TextField name="fonction" label={t('compte.fonction')} value={fonction} onChange={setFonction} help={t('compte.facultatif')} />
        </div>
      </div>
      <div>
        <Button variant="primary" type="submit" icon="arrow-right" loading={enCours}>{t(charteEnSuite ? 'premierAcces.continuer' : 'premierAcces.fiche.valider')}</Button>
      </div>
    </form>
  )
}

function EtapeCharte({ onAccepter }: { onAccepter: () => Promise<void> }) {
  const { t } = useTranslation()
  const [charte, setCharte] = useState(false)
  const [erreur, setErreur] = useState<string | undefined>()
  const [enCours, setEnCours] = useState(false)
  const soumettre = async (e: FormEvent) => {
    e.preventDefault()
    setErreur(charte ? undefined : t('premierAcces.fiche.charte.requise'))
    if (!charte) return
    setEnCours(true)
    try {
      await onAccepter()
    } finally {
      setEnCours(false)
    }
  }
  return (
    <form className="gm-col" onSubmit={(e) => void soumettre(e)} aria-labelledby="gm-premier-charte" noValidate>
      <Titre id="gm-premier-charte" cle="premierAcces.charte.titre" />
      <Panel>
        <p className="gm-section-desc">{t('premierAcces.charte.texte')}</p>
      </Panel>
      <Checkbox name="charte" label={t('premierAcces.charte.accepter')} checked={charte} onChange={setCharte} error={erreur} />
      <div>
        <Button variant="primary" type="submit" icon="arrow-right" loading={enCours}>{t('premierAcces.fiche.valider')}</Button>
      </div>
    </form>
  )
}

// Connexion de fin de premier accès : nouveau mot de passe (connu de l'assistant s'il vient d'être choisi)
// et un code de l'application d'authentification ; la passerelle ouvre alors la session complète.
function EtapeConnexion({ identifiant, motDePasseConnu, onConnecte }: { identifiant: string; motDePasseConnu: string | null; onConnecte: () => void }) {
  const { t } = useTranslation()
  const { service, couche } = useConnexion()
  const [motDePasse, setMotDePasse] = useState('')
  const [code, setCode] = useState('')
  const [champs, setChamps] = useState<{ motDePasse?: string; code?: string }>({})
  const [erreur, setErreur] = useState<MessageErreur | null>(null)
  const [enCours, setEnCours] = useState(false)
  const soumettre = async (e: FormEvent) => {
    e.preventDefault()
    setErreur(null)
    const locales = {
      motDePasse: motDePasseConnu || motDePasse ? undefined : t('connexion.motDePasse.requis'),
      code: CODE.test(code) ? undefined : t('connexion.code.format'),
    }
    setChamps(locales)
    if (locales.motDePasse || locales.code) return
    setEnCours(true)
    try {
      const r = await service.connecter({ identifiant, motDePasse: motDePasseConnu ?? motDePasse, code, couche })
      if (r.etat === 'CONNECTE') onConnecte()
      else setErreur({ texte: t('premierAcces.connexion.incomplet') })
    } catch (x) {
      const code = estApiError(x) ? x.code : ''
      setErreur({
        texte: t(`premierAcces.connexion.erreur.${code}`, { defaultValue: t(`connexion.erreur.${code}`, { defaultValue: t('connexion.erreur') }) }),
        correlationId: estApiError(x) ? x.correlationId || undefined : undefined,
      })
    } finally {
      setMotDePasse('')
      setCode('')
      setEnCours(false)
    }
  }
  return (
    <form className="gm-col" onSubmit={(e) => void soumettre(e)} aria-labelledby="gm-premier-connexion" noValidate>
      <Titre id="gm-premier-connexion" cle="premierAcces.connexion.titre" />
      <p className="gm-section-desc">{t(motDePasseConnu ? 'premierAcces.connexion.texte' : 'premierAcces.connexion.texteMotDePasse', { identifiant })}</p>
      {erreur ? <InlineAlert level="crit" title={erreur.texte} correlationId={erreur.correlationId} /> : null}
      {motDePasseConnu ? null : (
        <div style={{ maxWidth: 420 }}>
          <TextField name="motDePasse" type="password" label={t('connexion.motDePasse')} value={motDePasse} onChange={setMotDePasse} required autoComplete="current-password" error={champs.motDePasse} />
        </div>
      )}
      <div style={{ maxWidth: 320 }}>
        <TextField name="code" label={t('connexion.code')} value={code} onChange={(v) => setCode(v.replace(/\D/g, '').slice(0, 6))} required mono
          inputMode="numeric" maxLength={6} autoComplete="one-time-code" help={t('connexion.code.aide')} error={champs.code} />
      </div>
      <div>
        <Button variant="primary" type="submit" icon="key-round" loading={enCours}>{t('premierAcces.connexion.action')}</Button>
      </div>
    </form>
  )
}

export default function PremierAccesPage({ moi, onTermine }: { moi: Moi; onTermine: () => void }) {
  const { t } = useTranslation()
  const prefs = usePreferences()
  const session = useSession()
  const message = useMessageErreur()
  const configurer = usePremiereConfiguration()
  // Étapes figées à l'ouverture : l'avancement ne dépend pas des relectures du compte. Dans la session
  // restreinte du premier accès, la connexion avec le nouveau mot de passe termine l'assistant.
  const [etapes] = useState<Etape[]>(() => {
    const e: Etape[] = etapesAssistant(moi.etapesRestantes)
    return e.length > 0 && session?.restreinte ? [...e, 'CONNEXION'] : e
  })
  const [index, setIndex] = useState(0)
  const [erreur, setErreur] = useState<MessageErreur | null>(null)
  // Nouveau mot de passe, en mémoire le temps de l'assistant, pour la connexion finale ; effacé ensuite.
  const [motDePasse, setMotDePasse] = useState<string | null>(null)
  const fiche = useRef<Fiche | null>(null)
  const courante = etapes[index]
  useEffect(() => {
    if (etapes.length === 0) onTermine()
  }, [etapes, onTermine])
  const suivant = () => {
    setErreur(null)
    if (index + 1 < etapes.length) setIndex(index + 1)
    else onTermine()
  }

  const enregistrerFiche = async (f: Fiche) => {
    setErreur(null)
    try {
      await configurer.mutateAsync({ ...f, charteAcceptee: true })
      prefs.changerLangue(f.langue)
      suivant()
    } catch (x) {
      setErreur(message(x))
    }
  }
  const surFiche = async (f: Fiche) => {
    fiche.current = f
    if (etapes.includes('CHARTE')) suivant()
    else await enregistrerFiche(f)
    return true
  }
  const accepterCharte = () => {
    const c = moi.compte
    return enregistrerFiche(fiche.current ?? { telephone: c.telephone ?? '', langue: langueValide(c.langue) ?? 'fr', fonction: c.fonction })
  }
  const connecte = () => {
    setMotDePasse(null)
    navigationDocument.vers(retourCourant() ?? '/')
  }

  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-5)', maxWidth: 880 }}>
      <PageHeader title={t('premierAcces.titre')} subtitle={t('premierAcces.bienvenue', { nom: moi.compte.nomAffiche })} />
      <nav aria-label={t('premierAcces.etapes')}>
        <Stepper steps={etapes.map((e, i) => ({ label: t(`premierAcces.etape.${e}`), state: i < index ? 'done' : i === index ? 'current' : 'todo' }))} />
      </nav>
      <Card>
        {erreur ? <div style={{ marginBottom: 'var(--gm-space-4)' }}><InlineAlert level="crit" title={erreur.texte} correlationId={erreur.correlationId} /></div> : null}
        {courante === 'MOT_DE_PASSE' ? <EtapeMotDePasse onFait={(n) => { setMotDePasse(n); suivant() }} /> : null}
        {courante === 'SECOND_FACTEUR' ? <EtapeTotp onFait={suivant} /> : null}
        {courante === 'FICHE' ? <EtapeFiche moi={moi} charteEnSuite={etapes.includes('CHARTE')} onFiche={surFiche} /> : null}
        {courante === 'CHARTE' ? <EtapeCharte onAccepter={accepterCharte} /> : null}
        {courante === 'CONNEXION' ? <EtapeConnexion identifiant={moi.compte.identifiant} motDePasseConnu={motDePasse} onConnecte={connecte} /> : null}
      </Card>
    </div>
  )
}
