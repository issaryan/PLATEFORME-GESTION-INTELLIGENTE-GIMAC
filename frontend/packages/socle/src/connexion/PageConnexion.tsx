import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useSearchParams } from 'react-router'
import { Button, InlineAlert, Link, TextField } from '@gimac/design-system'
import logoCouleur from '@gimac/design-system/assets/logo/gimac-couleur.png'
import logoBlanc from '@gimac/design-system/assets/logo/gimac-blanc.png'
import { estApiError } from '../http/errors'
import { CONNEXION, type Couche } from '../http/prefixes'
import { LANGUES } from '../i18n/i18n'
import { usePreferences } from '../enveloppe/usePreferences'
import { navigation, retourSur } from '../session/deconnexion'
import { useConnexion } from './contexte'
import type { ServiceConnexion } from './modele'
import { ListeViolations, RappelPolitique } from './MotDePasse'
import { violationsDe } from './politique'
import './connexion.css'

export type PageConnexionProps = {
  couche: Couche
  /** Page ouverte après la connexion, à défaut d'un retour demandé : tableau de bord de la couche. */
  destination?: string
  /** Service de connexion ; à défaut, celui du ConnexionProvider (ou la passerelle). */
  service?: ServiceConnexion
  /** Erreur signalée par l'application, avec son identifiant de corrélation. */
  erreur?: { message: string; correlationId?: string }
}

type Erreur = { texte: string; correlationId?: string; violations?: string[]; code?: string }

/** Motif de trame (§5.6) : losanges et chevrons sur la grille de 8 px, période de 32 px, en tuile répétée. */
function Trame({ id }: { id: string }) {
  return (
    <svg className="gm-tuile-trame" aria-hidden="true">
      <defs>
        <pattern id={id} width="32" height="32" patternUnits="userSpaceOnUse">
          <path d="M16 0 L32 16 L16 32 L0 16 Z" />
          <path d="M0 8 L8 0 M24 32 L32 24 M0 24 L8 32 M24 0 L32 8" />
        </pattern>
      </defs>
      <rect width="100%" height="100%" fill={`url(#${id})`} />
    </svg>
  )
}

const format = (d: Date, fuseau: string) =>
  d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit', timeZone: fuseau })

/** Message d'une erreur de connexion par son code (fr, en, es), identifiant de corrélation et violations. */
function useErreurConnexion() {
  const { t } = useTranslation()
  return (e: unknown, prefixe = 'connexion.erreur'): Erreur => {
    if (!estApiError(e)) return { texte: t('connexion.erreur') }
    const generique = t(`erreur.${e.code}`, { defaultValue: t('connexion.erreur') })
    return {
      texte: t(`${prefixe}.${e.code}`, { defaultValue: t(`connexion.erreur.${e.code}`, { defaultValue: generique }) }),
      correlationId: e.correlationId || undefined,
      violations: violationsDe(e),
      code: e.code,
    }
  }
}

function Alerte({ erreur }: { erreur: Erreur | null }) {
  const { t } = useTranslation()
  if (!erreur) return null
  return (
    <InlineAlert level="crit" title={erreur.texte} correlationId={erreur.correlationId}>
      {erreur.violations?.length ? <><span>{t('motDePasse.violations')}</span><ListeViolations violations={erreur.violations} /></> : null}
    </InlineAlert>
  )
}

const CODE = /^[0-9]{6}$/

// Identifiant et mot de passe, puis code à six chiffres (CODE_REQUIS). Le mot de passe ne vit que dans
// l'état de ce formulaire, le temps de la saisie du code ; il est effacé dès la réponse ou l'abandon.
function FormulaireConnexion({ service, couche, destination, erreurApplication }: {
  service: ServiceConnexion
  couche: Couche
  destination: string
  erreurApplication?: PageConnexionProps['erreur']
}) {
  const { t } = useTranslation()
  const [parametres] = useSearchParams()
  const message = useErreurConnexion()
  const [etape, setEtape] = useState<'IDENTIFIANTS' | 'CODE'>('IDENTIFIANTS')
  const [identifiant, setIdentifiant] = useState('')
  const [motDePasse, setMotDePasse] = useState('')
  const [code, setCode] = useState('')
  const [champs, setChamps] = useState<{ identifiant?: string; motDePasse?: string; code?: string }>({})
  const [erreur, setErreur] = useState<Erreur | null>(erreurApplication ? { texte: erreurApplication.message, correlationId: erreurApplication.correlationId } : null)
  const [enCours, setEnCours] = useState(false)
  const motif = parametres.get('motif')
  const minutes = Number(parametres.get('minutes')) || 30

  const recommencer = () => {
    setEtape('IDENTIFIANTS')
    setMotDePasse('')
    setCode('')
    setChamps({})
    setErreur(null)
  }

  const soumettre = async (e: FormEvent) => {
    e.preventDefault()
    setErreur(null)
    const locales = etape === 'IDENTIFIANTS'
      ? { identifiant: identifiant.trim() ? undefined : t('connexion.identifiant.requis'), motDePasse: motDePasse ? undefined : t('connexion.motDePasse.requis') }
      : { code: CODE.test(code) ? undefined : t('connexion.code.format') }
    setChamps(locales)
    if (Object.values(locales).some(Boolean)) return
    setEnCours(true)
    try {
      const r = await service.connecter({ identifiant: identifiant.trim(), motDePasse, couche, ...(etape === 'CODE' ? { code } : {}) })
      if (r.etat === 'CODE_REQUIS' && etape === 'IDENTIFIANTS') {
        setEtape('CODE')
        return
      }
      if (r.etat === 'CODE_REQUIS') {
        setCode('')
        setErreur({ texte: t('connexion.erreur.code.CNX_IDENTIFIANTS_INVALIDES') })
        return
      }
      // CONNECTE, ou PREMIER_ACCES (session restreinte : l'assistant s'ouvre sur la page demandée).
      setMotDePasse('')
      setCode('')
      navigation.vers(destination)
    } catch (x) {
      setErreur(message(x, etape === 'CODE' ? 'connexion.erreur.code' : 'connexion.erreur'))
      if (etape === 'CODE') setCode('')
      else setMotDePasse('')
    } finally {
      setEnCours(false)
    }
  }

  return (
    <form className="gm-col gm-acces-formulaire" onSubmit={(e) => void soumettre(e)} noValidate aria-label={t('connexion.titre')}>
      {motif && !erreur ? <InlineAlert level={motif === 'deconnexion' ? 'info' : 'warn'} title={t(`connexion.motif.${motif}`, { minutes, defaultValue: '' }) || undefined} /> : null}
      <Alerte erreur={erreur} />
      {etape === 'IDENTIFIANTS' ? (
        <>
          <TextField name="identifiant" label={t('connexion.identifiant')} value={identifiant} onChange={setIdentifiant} required autoComplete="username" error={champs.identifiant} />
          <TextField name="motDePasse" type="password" label={t('connexion.motDePasse')} value={motDePasse} onChange={setMotDePasse} required autoComplete="current-password" error={champs.motDePasse} />
          <div className="gm-row gm-acces-actions">
            <Button variant="primary" size="lg" type="submit" icon="key-round" loading={enCours}>{t('connexion.action')}</Button>
            <Link href={`${CONNEXION}/mot-de-passe-oublie`}>{t('connexion.oubli.lien')}</Link>
          </div>
        </>
      ) : (
        <>
          <p style={{ margin: 0 }}>{t('connexion.code.texte', { identifiant: identifiant.trim() })}</p>
          <TextField name="code" label={t('connexion.code')} value={code} onChange={(v) => setCode(v.replace(/\D/g, '').slice(0, 6))} required mono
            inputMode="numeric" maxLength={6} autoComplete="one-time-code" help={t('connexion.code.aide')} error={champs.code} />
          <div className="gm-row gm-acces-actions">
            <Button variant="primary" size="lg" type="submit" icon="shield-check" loading={enCours}>{t('connexion.code.action')}</Button>
            <Button variant="ghost" onClick={recommencer}>{t('connexion.code.recommencer')}</Button>
          </div>
        </>
      )}
    </form>
  )
}

// Mot de passe oublié : la réponse est toujours la même (202), que le compte existe ou non.
function MotDePasseOublie({ service }: { service: ServiceConnexion }) {
  const { t } = useTranslation()
  const message = useErreurConnexion()
  const [identifiant, setIdentifiant] = useState('')
  const [requis, setRequis] = useState<string | undefined>()
  const [envoye, setEnvoye] = useState(false)
  const [erreur, setErreur] = useState<Erreur | null>(null)
  const [enCours, setEnCours] = useState(false)
  const soumettre = async (e: FormEvent) => {
    e.preventDefault()
    setErreur(null)
    setRequis(identifiant.trim() ? undefined : t('connexion.oubli.requis'))
    if (!identifiant.trim()) return
    setEnCours(true)
    try {
      await service.motDePasseOublie(identifiant.trim())
      setEnvoye(true)
    } catch (x) {
      setErreur(message(x))
    } finally {
      setEnCours(false)
    }
  }
  return (
    <form className="gm-col gm-acces-formulaire" onSubmit={(e) => void soumettre(e)} noValidate aria-labelledby="gm-oubli-titre">
      <h2 id="gm-oubli-titre" className="gm-type-h3" style={{ margin: 0 }}>{t('connexion.oubli.titre')}</h2>
      {envoye ? (
        <InlineAlert level="info" title={t('connexion.oubli.envoye')} />
      ) : (
        <>
          <p style={{ margin: 0 }}>{t('connexion.oubli.texte')}</p>
          <Alerte erreur={erreur} />
          <TextField name="identifiant" label={t('connexion.oubli.identifiant')} value={identifiant} onChange={setIdentifiant} required autoComplete="username" error={requis} />
          <div><Button variant="primary" type="submit" icon="send" loading={enCours}>{t('connexion.oubli.action')}</Button></div>
        </>
      )}
      <Link href={CONNEXION}>{t('connexion.retour')}</Link>
    </form>
  )
}

// Nouveau mot de passe depuis le lien du courriel (?jeton=…), à usage unique.
function NouveauMotDePasse({ service }: { service: ServiceConnexion }) {
  const { t } = useTranslation()
  const [parametres] = useSearchParams()
  const jeton = parametres.get('jeton') ?? ''
  const message = useErreurConnexion()
  const [nouveau, setNouveau] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [champs, setChamps] = useState<{ nouveau?: string; confirmation?: string }>({})
  const [erreur, setErreur] = useState<Erreur | null>(null)
  const [fait, setFait] = useState(false)
  const [enCours, setEnCours] = useState(false)
  const soumettre = async (e: FormEvent) => {
    e.preventDefault()
    setErreur(null)
    const locales = {
      nouveau: nouveau ? undefined : t('connexion.motDePasse.requis'),
      confirmation: confirmation === nouveau ? undefined : t('motDePasse.confirmation.differente'),
    }
    setChamps(locales)
    if (locales.nouveau || locales.confirmation) return
    setEnCours(true)
    try {
      await service.nouveauMotDePasse(jeton, nouveau)
      setFait(true)
    } catch (x) {
      setErreur(message(x))
    } finally {
      setNouveau('')
      setConfirmation('')
      setEnCours(false)
    }
  }
  return (
    <form className="gm-col gm-acces-formulaire" onSubmit={(e) => void soumettre(e)} noValidate aria-labelledby="gm-nouveau-titre">
      <h2 id="gm-nouveau-titre" className="gm-type-h3" style={{ margin: 0 }}>{t('connexion.nouveau.titre')}</h2>
      {fait ? (
        <>
          <InlineAlert level="info" title={t('connexion.nouveau.fait')} />
          <Link href={CONNEXION}>{t('connexion.nouveau.seConnecter')}</Link>
        </>
      ) : !jeton ? (
        <>
          <InlineAlert level="crit" title={t('connexion.erreur.CNX_JETON_INVALIDE')} />
          <Link href={`${CONNEXION}/mot-de-passe-oublie`}>{t('connexion.nouveau.redemander')}</Link>
        </>
      ) : (
        <>
          <Alerte erreur={erreur} />
          {erreur?.code === 'CNX_JETON_INVALIDE' ? <Link href={`${CONNEXION}/mot-de-passe-oublie`}>{t('connexion.nouveau.redemander')}</Link> : null}
          <TextField name="nouveauMotDePasse" type="password" label={t('motDePasse.nouveau')} value={nouveau} onChange={setNouveau} required autoComplete="new-password" error={champs.nouveau} />
          <RappelPolitique />
          <TextField name="confirmationMotDePasse" type="password" label={t('motDePasse.confirmation')} value={confirmation} onChange={setConfirmation} required autoComplete="new-password" error={champs.confirmation} />
          <div><Button variant="primary" type="submit" icon="key-round" loading={enCours}>{t('connexion.nouveau.action')}</Button></div>
          <Link href={CONNEXION}>{t('connexion.retour')}</Link>
        </>
      )}
    </form>
  )
}

// Connexion sur la plateforme (TRA-01, D-65) : une mosaïque tissée qui prolonge la couverture ; l'accès est
// l'un de ses blocs. Identifiant et mot de passe, puis code à six chiffres ; mot de passe oublié et
// nouveau mot de passe sous /connexion. La passerelle soumet les secrets à Keycloak, serveur à serveur :
// aucune page du fournisseur d'identité n'est ouverte. Langue et thème sont des tuiles de la mosaïque.
export function PageConnexion({ couche, destination, service, erreur }: PageConnexionProps) {
  const { t, i18n } = useTranslation()
  const p = usePreferences()
  const contexte = useConnexion()
  const svc = service ?? contexte.service
  const { pathname } = useLocation()
  const [parametres] = useSearchParams()
  const [maintenant, setMaintenant] = useState(() => new Date())
  useEffect(() => {
    const id = window.setInterval(() => setMaintenant(new Date()), 1000)
    return () => window.clearInterval(id)
  }, [])
  useEffect(() => {
    document.title = `GIMAC · ${t('connexion.titre')}`
  }, [t])
  const date = maintenant.toLocaleDateString(i18n.language, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'Africa/Douala' })
  const vue = pathname.endsWith('/mot-de-passe-oublie') ? 'oubli' : pathname.endsWith('/nouveau-mot-de-passe') ? 'nouveau' : 'connexion'
  const apres = retourSur(parametres.get('retour')) ?? destination ?? `/${couche}/tableau-de-bord`

  return (
    <div className="gm-mosaique gm-root">
      <div className="gm-tuile gm-tuile-action" aria-hidden="true" />
      <div className="gm-tuile gm-tuile-logo" aria-hidden="true" />
      <div className="gm-tuile gm-tuile-coche" aria-hidden="true" />
      <div className="gm-tuile gm-tuile-terre" aria-hidden="true"><Trame id="trame-terre" /></div>
      <div className="gm-tuile gm-tuile-relief" aria-hidden="true"><Trame id="trame-relief" /></div>

      <div className="gm-tuile gm-tuile-bande">
        <span className="gm-type-micro">{t(`connexion.couche.${couche}`)}</span>
        <span className="gm-type-micro">{t('connexion.classification')}</span>
      </div>

      <main className="gm-tuile gm-tuile-acces" aria-labelledby="gm-acces-titre">
        <img src={p.theme === 'dark' ? logoBlanc : logoCouleur} alt="GIMAC" />
        <h1 id="gm-acces-titre" className="gm-acces-nom">
          GIMAC <span>DES</span>
        </h1>
        {vue === 'connexion' ? <p className="gm-acces-texte">{t('connexion.explication')}</p> : null}
        {vue === 'oubli' ? <MotDePasseOublie service={svc} /> : null}
        {vue === 'nouveau' ? <NouveauMotDePasse service={svc} /> : null}
        {vue === 'connexion' ? <FormulaireConnexion service={svc} couche={couche} destination={apres} erreurApplication={erreur} /> : null}
        {vue === 'connexion' ? <p className="gm-field-help" style={{ margin: 0 }}>{t('connexion.cle')}</p> : null}
      </main>

      <section className="gm-tuile gm-tuile-instrument" aria-label={t('connexion.heure')}>
        <span className="gm-type-micro">{date}</span>
        <time className="gm-instrument-heure" dateTime={maintenant.toISOString()}>{format(maintenant, 'Africa/Douala')}</time>
        <span className="gm-instrument-utc">UTC {format(maintenant, 'UTC')}</span>
      </section>

      <section className="gm-tuile gm-tuile-choix gm-tuile-langue" aria-label={t('shell.langue')}>
        <span className="gm-type-micro">{t('shell.langue')}</span>
        <div className="gm-choix" role="group" aria-label={t('shell.langue')}>
          {LANGUES.map((l) => (
            <button key={l} type="button" lang={l} aria-pressed={p.langue === l} onClick={() => p.changerLangue(l)}>
              {l.toUpperCase()}
            </button>
          ))}
        </div>
      </section>

      <section className="gm-tuile gm-tuile-choix gm-tuile-theme" aria-label={t('shell.theme')}>
        <span className="gm-type-micro">{t('shell.theme')}</span>
        <div className="gm-choix" role="group" aria-label={t('shell.theme')}>
          <button type="button" aria-pressed={p.theme === 'light'} onClick={() => p.changerTheme('light')}>{t('shell.theme.clair')}</button>
          <button type="button" aria-pressed={p.theme === 'dark'} onClick={() => p.changerTheme('dark')}>{t('shell.theme.sombre.court')}</button>
        </div>
      </section>
    </div>
  )
}
