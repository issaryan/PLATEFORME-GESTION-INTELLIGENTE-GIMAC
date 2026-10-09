import { lazy, Suspense, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Navigate, Route, Routes, useLocation } from 'react-router'
import { Skeleton } from '@gimac/design-system'
import {
  Aiguillage, ConnexionProvider, Enveloppe, GardeSession, NotificationsPage, NotificationsProvider, PageConnexion, PageDeconnexion, SessionActive, aPermission, adresseConnexion, configureHttp,
  connexionHttp, lirePreference, lireSessionDemo, notificationsHttp, openLiveChannel, surNonAutorise, type EtatCanal, type LecteurApercu, type LiveTopic, type ServiceNotifications, type Session,
} from '@gimac/socle'
import { GardePremierAcces, MonComptePage, useMonAvatar, monCompteHttp, monCompteSimule } from '@gimac/compte'
import {
  CatalogueFonctionnalitesProvider, GardeFonctionnalite, PERM, lecteurApercu, navigationDepuis, navigationRepli, notificationsDemo, servicesHttp, servicesSimules, sessionDemoDuRole, useNonLus,
  useSourcesRecherche, useTraduire, type EtatCatalogue,
} from '@gimac/lot1'
import { Fournisseurs, creerClientRequetes, servicesConsoleHttp, servicesConsoleSimules, useFonctionnalites, useSourceComptes } from '@gimac/administration'
import { CLE_PREMIER_ACCES_DEMO, CLE_PROFIL_DEMO, ECRANS_CONSOLE, PROFILS_DEMO } from './navigation'
import { connexionDemo } from './connexionDemo'

// Console d'administration et d'audit (couche « console », D-46, D-61, D-62) : le super administrateur,
// le RSSI et chaque utilisateur configurent tout depuis la plateforme, sans ouvrir la console du
// fournisseur d'identité. Services réels par la passerelle par défaut, simulés seulement si VITE_SERVICES=simule (tests
// de bout en bout, démonstrations) ; session lue sur /api/session, second facteur exigé ; premier accès avant tout écran.
const TableauDeBordPage = lazy(() => import('@gimac/administration/pages/TableauDeBordPage'))
const ComptesPage = lazy(() => import('@gimac/administration/pages/ComptesPage'))
const ComptePage = lazy(() => import('@gimac/administration/pages/ComptePage'))
const CreationComptePage = lazy(() => import('@gimac/administration/pages/CreationComptePage'))
const JournalPage = lazy(() => import('@gimac/administration/pages/JournalPage'))
const PolitiqueSecuritePage = lazy(() => import('@gimac/administration/pages/PolitiqueSecuritePage'))
const CourrielsPage = lazy(() => import('@gimac/administration/pages/MessageriePage'))
// Messagerie instantanée de travail (D-64), commune au DES : tout membre interne, dont le RSSI et le super administrateur.
const MessagerieInstantanee = lazy(() => import('@gimac/lot1/pages/des/messagerie/MessageriePage'))
const ProfilsPage = lazy(() => import('@gimac/administration/pages/ProfilsPage'))
// Pouvoirs hiérarchiques (D-81) : demandes des chefs à traiter, matrice des pouvoirs, organisation (responsable d'unité).
const DemandesPage = lazy(() => import('@gimac/administration/pages/DemandesPage'))
const MatricePouvoirsPage = lazy(() => import('@gimac/administration/pages/MatricePouvoirsPage'))
const OrganisationPage = lazy(() => import('@gimac/administration/pages/OrganisationPage'))
// Organigramme (D-86 §3) : unités, chefs, membres et n+1, pouvoirs effectifs ; gestes par glisser-déposer ou au clavier.
const OrganigrammePage = lazy(() => import('@gimac/administration/pages/OrganigrammePage'))
const ConfigurationPlateformePage = lazy(() => import('@gimac/administration/pages/ConfigurationPlateformePage'))
const JournalisationPage = lazy(() => import('@gimac/administration/pages/JournalisationPage'))
// Exploitation de l'infrastructure (D-78) : charges, journaux, gestes confirmés par la clé de sécurité.
const InfrastructurePage = lazy(() => import('@gimac/administration/pages/InfrastructurePage'))
const SupervisionPage = lazy(() => import('@gimac/administration/pages/SupervisionPage'))

const VERSION = import.meta.env.VITE_VERSION ?? '0.1.0'

function profilDemo() {
  const adresse = typeof window === 'undefined' ? null : new URLSearchParams(window.location.search).get('profil')
  // Profil de la session ouverte par la connexion simulée (D-65) d'abord : une session dont le profil n'a pas sa
  // place dans la console (profil externe, rôle d'une autre application) garde son rôle et sera aiguillée (D-79,
  // recette ACC-01) ; jamais de profil par défaut pour elle.
  const ouverte = lireSessionDemo()?.role
  if (ouverte && !PROFILS_DEMO[ouverte]) {
    const s = sessionDemoDuRole(ouverte)
    if (s) return { role: ouverte, identifiant: s.identifiant, nom: s.nom, sujet: s.sujet, permissions: s.permissions, institutionId: s.institutionId }
  }
  const demande = ouverte ?? (adresse && PROFILS_DEMO[adresse] ? adresse : null) ?? lirePreference(CLE_PROFIL_DEMO) ?? import.meta.env.VITE_PROFIL_DEMO
  const role = demande && PROFILS_DEMO[demande] ? demande : 'ROLE_SUPER_ADMIN'
  return { role, ...PROFILS_DEMO[role], institutionId: '' }
}
function sessionDemo(): Session {
  const p = profilDemo()
  const demo = lireSessionDemo()
  return {
    sujet: p.sujet, nom: p.nom, identifiant: demo?.identifiant || p.identifiant, roles: [p.role], permissions: p.permissions, institutionId: p.institutionId, acr: '2', doubleFacteur: true,
    ...(demo?.premierAcces ? { restreinte: true } : {}),
  }
}

/**
 * Écran d'une fonctionnalité du catalogue servi par MS-IAM (D-94) : ouvert si la configuration le rend visible et que
 * les permissions satisfont son prédicat (et détiennent `droit`) ; refus explicite, la passerelle refait le contrôle.
 */
const G = (cle: string, page: ReactNode, droit?: string) => <GardeFonctionnalite cle={cle} droit={droit}>{page}</GardeFonctionnalite>

/** État du catalogue des fonctionnalités : lu sur /bff/console/iam/configuration/fonctionnalites ; échec : repli prudent. */
function useEtatCatalogue(actif: boolean): EtatCatalogue {
  const q = useFonctionnalites(actif)
  // Réponse qui n'est pas une liste : tenue pour indisponible (repli prudent), comme un échec.
  return useMemo(() => (Array.isArray(q.data) ? { etat: 'pret', fonctionnalites: q.data } : q.isError || q.data ? { etat: 'indisponible', fonctionnalites: [] } : { etat: 'chargement', fonctionnalites: [] }), [q.data, q.isError])
}

/**
 * Canal poussé de la console (services réels) : nouveaux messages de la messagerie instantanée (D-64) et
 * notifications de la plateforme (D-80, sujet « notification », remis au seul destinataire).
 */
function CanalMessagerie({ sujets, onEtat }: { sujets: LiveTopic[]; onEtat: (e: EtatCanal) => void }) {
  const qc = useQueryClient()
  const cle = sujets.join(',')
  useEffect(() => (cle ? openLiveChannel('console', cle.split(',') as LiveTopic[], qc, onEtat, { baseMs: 1000, maxMs: 30_000 }) : undefined), [cle, qc, onEtat])
  return null
}

/** Notifications de la plateforme (D-80) : MS-NOTIFICATION (ou sa simulation) et l'aperçu lu auprès de la messagerie. */
type Notifications = { service: ServiceNotifications; apercu: LecteurApercu }

function Pages({ session, correlationId, notifications }: { session: Session; correlationId?: string; notifications: Notifications }) {
  // Photo de profil du titulaire (docs/lot1/mon-compte.md §4) : menu utilisateur de la barre du haut.
  const moi = useMonAvatar()
  const p = session.permissions
  const messagerie = aPermission(p, PERM.messageLire)
  const avecNotifications = aPermission(p, PERM.notificationsLire)
  const [etatCanal, setEtatCanal] = useState<EtatCanal | undefined>()
  // Messages non lus (D-64) : pastille de l'entrée « Messagerie », rafraîchie par le canal poussé.
  const nonLus = useNonLus(messagerie)
  // Recherche globale (Ctrl+K) : comptes et conversations de la messagerie.
  const comptes = useSourceComptes(p)
  const conversations = useSourcesRecherche(p, '/console')
  const recherche = useMemo(() => [...comptes, ...conversations], [comptes, conversations])
  // Navigation de la console dérivée du catalogue servi (D-94) ; l'accueil seul tant qu'il n'est pas servi.
  const catalogue = useEtatCatalogue(!session.restreinte)
  const tr = useTraduire()
  const navigation = useMemo(() => (catalogue.etat === 'pret' ? navigationDepuis(catalogue.fonctionnalites, 'console', ECRANS_CONSOLE, tr, 'console.nav.groupe.') : navigationRepli('console')), [catalogue, tr])
  return (
    <CatalogueFonctionnalitesProvider valeur={catalogue}>
    <GardePremierAcces>
      {(assistant) => (
        // Cloche, fenêtres surgissantes et page des notifications (D-80) : NOT_INBOX_READ, hors premier accès.
        <NotificationsProvider service={notifications.service} couche="console" actif={!assistant && avecNotifications} apercu={notifications.apercu}>
        <Enveloppe avatar={moi?.src} fonction={moi?.fonction} couche="console" navigation={assistant ? [] : navigation} permissions={p} utilisateur={session.nom} version={VERSION}
          badges={{ consoleMessagerie: nonLus.data?.total ?? 0 }} etatCanal={etatCanal} correlationId={correlationId} espacePersonnel="/console" recherche={assistant ? undefined : recherche}>
          <CanalMessagerie onEtat={setEtatCanal}
            sujets={assistant || import.meta.env.VITE_SERVICES === 'simule' ? [] : [...(messagerie ? ['collaboration', 'presence'] as const : []), ...(avecNotifications ? ['notification'] as const : [])]} />
          <Suspense fallback={<Skeleton height={32} />}>
            <Routes>
              <Route path="deconnexion" element={<PageDeconnexion />} />
              {assistant ? (
                <Route path="*" element={assistant} />
              ) : (
                <>
                  <Route path="tableau-de-bord" element={<TableauDeBordPage />} />
                  <Route path="comptes" element={G('consoleComptes', <ComptesPage />)} />
                  <Route path="comptes/nouveau" element={G('consoleComptes', <CreationComptePage />, 'IAM_ACCOUNT_CREATE')} />
                  <Route path="comptes/:id" element={G('consoleComptes', <ComptePage />)} />
                  <Route path="journal" element={G('consoleJournal', <JournalPage />)} />
                  <Route path="profils" element={G('consoleProfils', <ProfilsPage />)} />
                  <Route path="demandes" element={G('consoleDemandes', <DemandesPage />)} />
                  <Route path="matrice-pouvoirs" element={G('consoleMatrice', <MatricePouvoirsPage />)} />
                  <Route path="organisation" element={G('consoleOrganisation', <OrganisationPage />)} />
                  <Route path="organigramme" element={G('consoleOrganigramme', <OrganigrammePage />)} />
                  <Route path="politique-securite" element={G('consolePolitique', <PolitiqueSecuritePage />)} />
                  <Route path="messagerie" element={G('consoleMessagerie', <MessagerieInstantanee couche="console" />)} />
                  <Route path="notifications" element={G('consoleNotifications', <NotificationsPage />)} />
                  <Route path="plateforme/courriels" element={G('consoleCourriels', <CourrielsPage />)} />
                  <Route path="plateforme/configuration" element={G('consoleConfiguration', <ConfigurationPlateformePage />)} />
                  <Route path="plateforme/journalisation" element={G('consoleJournalisation', <JournalisationPage />)} />
                  <Route path="plateforme/supervision" element={G('consoleSupervision', <SupervisionPage />)} />
                  <Route path="plateforme/infrastructure" element={G('consoleInfrastructure', <InfrastructurePage />)} />
                  <Route path="mon-compte" element={<MonComptePage />} />
                  <Route path="*" element={<Navigate to="/console/tableau-de-bord" replace />} />
                </>
              )}
            </Routes>
          </Suspense>
        </Enveloppe>
        </NotificationsProvider>
      )}
    </GardePremierAcces>
    </CatalogueFonctionnalitesProvider>
  )
}

function AvecSession({ children }: { children: (s: Session) => ReactNode }) {
  const ici = useLocation()
  // Lu ici et non reçu en propriété : constant à la construction, la branche simulée (profils et comptes de
  // démonstration) disparaît du paquet servi en production.
  if (import.meta.env.VITE_SERVICES === 'simule') {
    // Connexion simulée (D-65) : sans session de démonstration, écran de connexion.
    if (!lireSessionDemo()) return <Navigate to={adresseConnexion({ retour: ici.pathname + ici.search })} replace />
    const s = sessionDemo()
    return <SessionActive session={s} prolonger={async () => undefined}><Aiguillage session={s} couche="console">{children(s)}</Aiguillage></SessionActive>
  }
  // Aiguillage (D-79) : la console n'accueille que les comptes portant une permission de console ; les autres
  // comptes internes sont conduits vers l'application interne, les comptes d'institution vers le portail.
  return <GardeSession>{(s) => <Aiguillage session={s} couche="console">{children(s)}</Aiguillage>}</GardeSession>
}

export default function App() {
  const reels = import.meta.env.VITE_SERVICES !== 'simule'
  const services = useMemo(() => {
    if (reels) return servicesConsoleHttp()
    const p = profilDemo()
    return servicesConsoleSimules({ acteur: { id: p.sujet, nom: p.nom, permissions: p.permissions } })
  }, [reels])
  const client = useMemo(() => creerClientRequetes(), [])
  // Messagerie instantanée (D-64) : /bff/console/collaboration, ou annuaire et conversations simulés ; notifications
  // de la plateforme (D-80) : /bff/console/notification, ou MS-NOTIFICATION simulé branché sur la messagerie simulée.
  const { messagerie, notifications } = useMemo(() => {
    if (reels) {
      const m = servicesHttp('console')
      return { messagerie: m, notifications: { service: notificationsHttp('console'), apercu: lecteurApercu(m) } }
    }
    const p = profilDemo()
    const m = servicesSimules({ utilisateur: { id: p.sujet, nom: p.nom } })
    return { messagerie: m, notifications: { service: notificationsDemo(m, { qc: client, moi: p.sujet, profils: [p.role] }), apercu: lecteurApercu(m) } }
  }, [reels, client])
  const compte = useMemo(() => {
    if (reels) return monCompteHttp('console')
    const p = profilDemo()
    const demo = lireSessionDemo()
    const identifiant = demo?.identifiant || p.identifiant
    const [prenom, ...nom] = p.nom.split(' ')
    return monCompteSimule({
      couche: 'console',
      premierAcces: demo?.premierAcces ?? lirePreference(CLE_PREMIER_ACCES_DEMO) === '1',
      etapes: demo?.etapes,
      compte: { id: p.sujet, identifiant, prenom, nom: nom.join(' '), nomAffiche: p.nom, email: `${identifiant}@gimac-afr.org`, profils: [p.role] },
    })
  }, [reels])
  const connexion = useMemo(() => (reels ? connexionHttp() : connexionDemo()), [reels])
  const [correlationId, setCorrelationId] = useState<string | undefined>()
  useEffect(() => {
    // Session absente ou close par la passerelle (401, CNX_SESSION_EXPIREE) : retour à la connexion (D-65).
    configureHttp({
      onCorrelation: setCorrelationId,
      onUnauthorized: async (e) => (reels ? surNonAutorise(e) : false),
    })
  }, [reels])
  return (
    <Fournisseurs services={services} compte={compte} messagerie={messagerie} clientRequetes={client}>
      <ConnexionProvider service={connexion} couche="console">
      <Routes>
        <Route path="/connexion/*" element={<PageConnexion couche="console" />} />
        <Route path="/console/*" element={<AvecSession>{(s) => <Pages session={s} correlationId={correlationId} notifications={notifications} />}</AvecSession>} />
        <Route path="*" element={<Navigate to="/console/tableau-de-bord" replace />} />
      </Routes>
      </ConnexionProvider>
    </Fournisseurs>
  )
}
