import { lazy, Suspense, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Navigate, Route, Routes, useLocation } from 'react-router'
import { useTranslation } from 'react-i18next'
import { EmptyState, Skeleton } from '@gimac/design-system'
import {
  ConnexionProvider, Enveloppe, GardeSession, PageConnexion, PageDeconnexion, SessionActive, aPermission, adresseConnexion, configureHttp, connexionHttp, lirePreference,
  lireSessionDemo, openLiveChannel, surNonAutorise, type EtatCanal, type Session,
} from '@gimac/socle'
import { GardePremierAcces, MonComptePage, useMonAvatar, monCompteHttp, monCompteSimule } from '@gimac/compte'
import { PERM, servicesHttp, servicesSimules, useNonLus, useSourcesRecherche } from '@gimac/lot1'
import { Fournisseurs, creerClientRequetes, servicesConsoleHttp, servicesConsoleSimules, useSourceComptes } from '@gimac/administration'
import { CLE_PREMIER_ACCES_DEMO, CLE_PROFIL_DEMO, NAVIGATION_CONSOLE, PROFILS_DEMO } from './navigation'
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
const ConfigurationPlateformePage = lazy(() => import('@gimac/administration/pages/ConfigurationPlateformePage'))
const JournalisationPage = lazy(() => import('@gimac/administration/pages/JournalisationPage'))

const VERSION = import.meta.env.VITE_VERSION ?? '0.1.0'

function profilDemo() {
  const adresse = typeof window === 'undefined' ? null : new URLSearchParams(window.location.search).get('profil')
  // Profil de la session ouverte par la connexion simulée (D-65) d'abord.
  const demande = lireSessionDemo()?.role ?? (adresse && PROFILS_DEMO[adresse] ? adresse : null) ?? lirePreference(CLE_PROFIL_DEMO) ?? import.meta.env.VITE_PROFIL_DEMO
  const role = demande && PROFILS_DEMO[demande] ? demande : 'ROLE_SUPER_ADMIN'
  return { role, ...PROFILS_DEMO[role] }
}
function sessionDemo(): Session {
  const p = profilDemo()
  const demo = lireSessionDemo()
  return {
    sujet: p.sujet, nom: p.nom, identifiant: demo?.identifiant || p.identifiant, roles: [p.role], permissions: p.permissions, institutionId: '', acr: '2', doubleFacteur: true,
    ...(demo?.premierAcces ? { restreinte: true } : {}),
  }
}

/** Écran réservé à une permission : sans elle, accès refusé explicite (la passerelle refait le contrôle). */
function Autorise({ permissions, exigee, children }: { permissions: string[]; exigee: string; children: ReactNode }) {
  const { t } = useTranslation()
  return aPermission(permissions, exigee) ? <>{children}</> : <EmptyState icon="lock" title={t('console.refuse.titre')} text={t('console.refuse.texte')} />
}

/** Canal poussé de la console (services réels) : nouveaux messages de la messagerie instantanée (D-64). */
function CanalMessagerie({ actif, onEtat }: { actif: boolean; onEtat: (e: EtatCanal) => void }) {
  const qc = useQueryClient()
  useEffect(() => (actif ? openLiveChannel('console', ['collaboration', 'presence'], qc, onEtat, { baseMs: 1000, maxMs: 30_000 }) : undefined), [actif, qc, onEtat])
  return null
}

function Pages({ session, correlationId }: { session: Session; correlationId?: string }) {
  // Photo de profil du titulaire (docs/lot1/mon-compte.md §4) : menu utilisateur de la barre du haut.
  const moi = useMonAvatar()
  const p = session.permissions
  const messagerie = aPermission(p, PERM.messageLire)
  const [etatCanal, setEtatCanal] = useState<EtatCanal | undefined>()
  // Messages non lus (D-64) : pastille de l'entrée « Messagerie », rafraîchie par le canal poussé.
  const nonLus = useNonLus(messagerie)
  // Recherche globale (Ctrl+K) : comptes et conversations de la messagerie.
  const comptes = useSourceComptes(p)
  const conversations = useSourcesRecherche(p, '/console')
  const recherche = useMemo(() => [...comptes, ...conversations], [comptes, conversations])
  return (
    <GardePremierAcces>
      {(assistant) => (
        <Enveloppe avatar={moi?.src} fonction={moi?.fonction} couche="console" navigation={assistant ? [] : NAVIGATION_CONSOLE} permissions={p} utilisateur={session.nom} version={VERSION}
          badges={{ messagerie: nonLus.data?.total ?? 0 }} etatCanal={etatCanal} correlationId={correlationId} espacePersonnel="/console" recherche={assistant ? undefined : recherche}>
          <CanalMessagerie actif={messagerie && !assistant && import.meta.env.VITE_SERVICES !== 'simule'} onEtat={setEtatCanal} />
          <Suspense fallback={<Skeleton height={32} />}>
            <Routes>
              <Route path="deconnexion" element={<PageDeconnexion />} />
              {assistant ? (
                <Route path="*" element={assistant} />
              ) : (
                <>
                  <Route path="tableau-de-bord" element={<TableauDeBordPage />} />
                  <Route path="comptes" element={<Autorise permissions={p} exigee="IAM_ACCOUNT_READ"><ComptesPage /></Autorise>} />
                  <Route path="comptes/nouveau" element={<Autorise permissions={p} exigee="IAM_ACCOUNT_CREATE"><CreationComptePage /></Autorise>} />
                  <Route path="comptes/:id" element={<Autorise permissions={p} exigee="IAM_ACCOUNT_READ"><ComptePage /></Autorise>} />
                  <Route path="journal" element={<Autorise permissions={p} exigee="IAM_JOURNAL_READ"><JournalPage /></Autorise>} />
                  <Route path="profils" element={<Autorise permissions={p} exigee="IAM_ACCOUNT_READ"><ProfilsPage /></Autorise>} />
                  <Route path="politique-securite" element={<Autorise permissions={p} exigee="PLATFORM_SECURITY_POLICY_READ"><PolitiqueSecuritePage /></Autorise>} />
                  <Route path="messagerie" element={<Autorise permissions={p} exigee={PERM.messageLire}><MessagerieInstantanee couche="console" /></Autorise>} />
                  <Route path="plateforme/courriels" element={<Autorise permissions={p} exigee="PLATFORM_MESSAGING_EDIT"><CourrielsPage /></Autorise>} />
                  <Route path="plateforme/configuration" element={<Autorise permissions={p} exigee="PLATFORM_CONFIG_READ"><ConfigurationPlateformePage /></Autorise>} />
                  <Route path="plateforme/journalisation" element={<Autorise permissions={p} exigee="PLATFORM_CONFIG_READ"><JournalisationPage /></Autorise>} />
                  <Route path="mon-compte" element={<MonComptePage />} />
                  <Route path="*" element={<Navigate to="/console/tableau-de-bord" replace />} />
                </>
              )}
            </Routes>
          </Suspense>
        </Enveloppe>
      )}
    </GardePremierAcces>
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
    return <SessionActive session={s} prolonger={async () => undefined}>{children(s)}</SessionActive>
  }
  return <GardeSession>{children}</GardeSession>
}

export default function App() {
  const reels = import.meta.env.VITE_SERVICES !== 'simule'
  const services = useMemo(() => {
    if (reels) return servicesConsoleHttp()
    const p = profilDemo()
    return servicesConsoleSimules({ acteur: { id: p.sujet, nom: p.nom, permissions: p.permissions } })
  }, [reels])
  // Messagerie instantanée (D-64) : /bff/console/collaboration, ou annuaire et conversations simulés.
  const messagerie = useMemo(() => {
    if (reels) return servicesHttp('console')
    const p = profilDemo()
    return servicesSimules({ utilisateur: { id: p.sujet, nom: p.nom } })
  }, [reels])
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
  const client = useMemo(() => creerClientRequetes(), [])
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
        <Route path="/console/*" element={<AvecSession>{(s) => <Pages session={s} correlationId={correlationId} />}</AvecSession>} />
        <Route path="*" element={<Navigate to="/console/tableau-de-bord" replace />} />
      </Routes>
      </ConnexionProvider>
    </Fournisseurs>
  )
}
