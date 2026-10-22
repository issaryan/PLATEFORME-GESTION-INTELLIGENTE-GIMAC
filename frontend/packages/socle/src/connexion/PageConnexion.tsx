import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useSearchParams } from 'react-router'
import { Button, Icon, IconButton, InlineAlert, Link, TextField } from '@gimac/design-system'
import logoCouleur from '@gimac/design-system/assets/logo/gimac-logo-couleur.png'
import logoBlanc from '@gimac/design-system/assets/logo/gimac-logo-blanc.png'
import { estApiError } from '../http/errors'
import { CONNEXION, type Couche } from '../http/prefixes'
import { usePreferences } from '../enveloppe/usePreferences'
import { Preferences } from '../enveloppe/Preferences'
import { Cernes, Marque, NOM_PLATEFORME } from './Decor'
import { navigation, retourSur } from '../session/deconnexion'
import { AIGUILLAGE_PLATEFORME, type HabillageConnexion, type ReglesAiguillage } from '../habillage'
import { reglageAiguillage } from '../session/Aiguillage'
import { lireSessionDemo } from '../session/demo'
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
  /** Décor, logo, nom et mention d'un produit hébergé (D-118) ; ceux de la plateforme à défaut. */
  habillage?: HabillageConnexion
  /** Règle d'aiguillage d'un produit hébergé (D-118) ; celle de la plateforme à défaut. */
  aiguillage?: ReglesAiguillage
}

type Erreur = { texte: string; correlationId?: string; violations?: string[]; code?: string }

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
function FormulaireConnexion({ service, couche, destination, erreurApplication, regles }: {
  service: ServiceConnexion
  couche: Couche
  destination: string
  erreurApplication?: PageConnexionProps['erreur']
  regles: ReglesAiguillage
}) {
  const { t } = useTranslation()
  const [parametres] = useSearchParams()
  const message = useErreurConnexion()
  const [etape, setEtape] = useState<'IDENTIFIANTS' | 'CODE'>('IDENTIFIANTS')
  // Identifiant prérempli quand une autre application y conduit le compte (aiguillage, D-79).
  const [identifiant, setIdentifiant] = useState(() => parametres.get('identifiant') ?? '')
  const [motDePasse, setMotDePasse] = useState('')
  const [code, setCode] = useState('')
  const [champs, setChamps] = useState<{ identifiant?: string; motDePasse?: string; code?: string }>({})
  const [erreur, setErreur] = useState<Erreur | null>(erreurApplication ? { texte: erreurApplication.message, correlationId: erreurApplication.correlationId } : null)
  const [enCours, setEnCours] = useState(false)
  // Départ vers l'application du compte (D-79) : message bref, puis changement d'application.
  const [aiguillage, setAiguillage] = useState<{ texte: string; adresse: string } | null>(null)
  const motif = parametres.get('motif')
  const depuis = parametres.get('depuis')
  const minutes = Number(parametres.get('minutes')) || 30

  useEffect(() => {
    if (!aiguillage) return
    const id = window.setTimeout(() => navigation.vers(aiguillage.adresse), reglageAiguillage.delaiMs)
    return () => window.clearTimeout(id)
  }, [aiguillage])

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
      // Compte connecté hors de son application (compte interne sans permission de console sur la console) :
      // conduit vers son tableau de bord, la session de la passerelle étant commune aux trois applications.
      const cible = r.etat === 'CONNECTE' ? regles.applicationDe(r.session, couche) : couche
      if (cible !== couche) {
        setAiguillage({ texte: t(`aiguillage.${couche}.${cible}`, { defaultValue: t(`aiguillage.vers.${cible}`) }), adresse: regles.adresse(cible, lireSessionDemo()) })
        return
      }
      navigation.vers(destination)
    } catch (x) {
      // Couche refusée par la passerelle : le compte relève d'une autre application, ouverte sur son écran de connexion.
      if (estApiError(x) && x.code === 'CNX_COUCHE_REFUSEE') {
        const cible = regles.apresRefus(couche)
        setMotDePasse('')
        setAiguillage({ texte: t(`aiguillage.refus.${cible}`), adresse: regles.adresseConnexion(cible, { identifiant, depuis: couche }) })
        return
      }
      setErreur(message(x, etape === 'CODE' ? 'connexion.erreur.code' : 'connexion.erreur'))
      if (etape === 'CODE') setCode('')
      else setMotDePasse('')
    } finally {
      setEnCours(false)
    }
  }

  return (
    <form className="gm-col gm-acces-formulaire" onSubmit={(e) => void soumettre(e)} noValidate aria-label={t('connexion.titre')}>
      {aiguillage ? (
        <InlineAlert level="info" title={aiguillage.texte}>
          <Link href={aiguillage.adresse}>{t('aiguillage.continuer')}</Link>
        </InlineAlert>
      ) : null}
      {motif && !erreur && !aiguillage ? (
        <InlineAlert level={motif === 'deconnexion' || motif === 'aiguillage' ? 'info' : 'warn'}
          title={t(motif === 'aiguillage' && depuis ? `connexion.motif.aiguillage.${depuis}` : `connexion.motif.${motif}`, { minutes, defaultValue: '' }) || undefined} />
      ) : null}
      <Alerte erreur={erreur} />
      {etape === 'IDENTIFIANTS' ? (
        <>
          <TextField name="identifiant" label={t('connexion.identifiant')} value={identifiant} onChange={setIdentifiant} required autoComplete="username" error={champs.identifiant} />
          <div className="gm-acces-mdp">
            <TextField name="motDePasse" type="password" label={t('connexion.motDePasse')} value={motDePasse} onChange={setMotDePasse} required autoComplete="current-password" error={champs.motDePasse} />
            <Link href={`${CONNEXION}/mot-de-passe-oublie`}>{t('connexion.oubli.lien')}</Link>
          </div>
          <Button variant="primary" size="lg" type="submit" icon="key-round" loading={enCours}>{t('connexion.action')}</Button>
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
      <h1 id="gm-oubli-titre" className="gm-acces-titre">{t('connexion.oubli.titre')}</h1>
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
      <h1 id="gm-nouveau-titre" className="gm-acces-titre">{t('connexion.nouveau.titre')}</h1>
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

/** Préférences de l'écran de connexion : langue (menu surgissant du globe) et bascule directe du thème. */
function OutilsConnexion() {
  const { t, i18n } = useTranslation()
  const p = usePreferences()
  const [ouvert, setOuvert] = useState(false)
  const ancre = useRef<HTMLSpanElement>(null)
  useEffect(() => {
    if (!ouvert) return
    const fermer = (e: MouseEvent | KeyboardEvent) => {
      if (e instanceof KeyboardEvent ? e.key === 'Escape' : !ancre.current?.contains(e.target as Node)) setOuvert(false)
    }
    document.addEventListener('mousedown', fermer)
    document.addEventListener('keydown', fermer)
    return () => {
      document.removeEventListener('mousedown', fermer)
      document.removeEventListener('keydown', fermer)
    }
  }, [ouvert])
  const sombre = p.theme === 'dark'
  return (
    <div className="gm-acces-outils">
      <span ref={ancre} className="gm-topbar-ancre">
        <button type="button" className="gm-acces-outil" aria-label={t('shell.preferences')} aria-haspopup="dialog" aria-expanded={ouvert} onClick={() => setOuvert(!ouvert)}>
          <Icon name="globe" size={18} />
          <span>{i18n.language.slice(0, 2).toUpperCase()}</span>
          <Icon name="chevron-down" size={16} />
        </button>
        {ouvert ? <div className="gm-popover gm-topbar-pop" role="dialog" aria-label={t('shell.preferences')}><Preferences /></div> : null}
      </span>
      <IconButton variant="ghost" icon={sombre ? 'sun' : 'moon'} label={sombre ? t('shell.theme.clair') : t('shell.theme.sombre')} onClick={() => p.changerTheme(sombre ? 'light' : 'dark')} />
    </div>
  )
}

// Connexion sur la plateforme (TRA-01, D-65, D-67) : courbes de niveau sur toute la page, carte d'accès à filet
// tricolore à gauche, nom de la plateforme en grand à droite. Identifiant et mot de passe, puis code à six
// chiffres ; mot de passe oublié et nouveau mot de passe sous /connexion. La passerelle soumet les secrets à
// Keycloak, serveur à serveur : aucune page du fournisseur d'identité n'est ouverte. Langue et thème en haut à
// droite ; couche, classification et heures de Douala et UTC en pied de page.
export function PageConnexion({ couche, destination, service, erreur, habillage: h, aiguillage = AIGUILLAGE_PLATEFORME }: PageConnexionProps) {
  const { t } = useTranslation()
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
    // D-101 : l'onglet porte simplement le nom du logiciel.
    document.title = h?.nom ?? NOM_PLATEFORME
  }, [h?.nom])
  const vue = pathname.endsWith('/mot-de-passe-oublie') ? 'oubli' : pathname.endsWith('/nouveau-mot-de-passe') ? 'nouveau' : 'connexion'
  const apres = retourSur(parametres.get('retour')) ?? destination ?? `/${couche}/tableau-de-bord`
  const sombre = p.theme === 'dark'

  return (
    <div className={h?.marque === null ? 'gm-acces gm-root gm-acces-centre' : 'gm-acces gm-root'}>
      {h?.fond ?? null}
      <header className="gm-acces-haut"><OutilsConnexion /></header>
      <div className="gm-acces-corps">
        <main className="gm-acces-carte" aria-labelledby={vue === 'connexion' ? 'gm-acces-titre' : undefined}>
          {h?.fond === undefined ? <Cernes /> : null}
          <div className="gm-filet" aria-hidden="true"><i /><i /><i /></div>
          <div className="gm-acces-contenu">
            <img className="gm-acces-logo" src={h?.logo ? (sombre ? h.logo.sombre : h.logo.clair) : sombre ? logoBlanc : logoCouleur} alt={h?.logo?.alt ?? 'GIMAC'} />
            {vue === 'connexion' ? <h1 id="gm-acces-titre" className="gm-acces-titre">{t('connexion.titre')}</h1> : null}
            {vue === 'oubli' ? <MotDePasseOublie service={svc} /> : null}
            {vue === 'nouveau' ? <NouveauMotDePasse service={svc} /> : null}
            {vue === 'connexion' ? <FormulaireConnexion service={svc} couche={couche} destination={apres} erreurApplication={erreur} regles={aiguillage} /> : null}
            {vue === 'connexion' ? <p className="gm-acces-note">{t('connexion.cle')}</p> : null}
          </div>
        </main>
        {h?.marque === undefined ? <Marque sombre={sombre} signature={t('connexion.signature')} /> : h.marque}
      </div>
      <footer className="gm-acces-pied">
        {/* DDR-22 : le portail des participants n'est pas d'usage interne. */}
        {h?.mention === '' ? null : <span>{h?.mention ?? `${t(`connexion.couche.${couche}`)} · ${t(couche === 'portail' ? 'connexion.classificationPortail' : 'connexion.classification')}`}</span>}
        <span className="gm-acces-heure" role="timer" aria-label={t('connexion.heure')}>
          <time dateTime={maintenant.toISOString()}>Douala {format(maintenant, 'Africa/Douala')}</time> · UTC {format(maintenant, 'UTC')}
        </span>
      </footer>
    </div>
  )
}
