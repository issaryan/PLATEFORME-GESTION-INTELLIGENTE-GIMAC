import { lazy, Suspense, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { useTranslation } from 'react-i18next'
import { EmptyState, Skeleton } from '@gimac/design-system'
import {
  CONNEXION, Enveloppe, GardeSession, PageConnexion, PageDeconnexion, SessionProvider, aPermission, configureHttp, lirePreference, type Session,
} from '@gimac/socle'
import { GardePremierAcces, MonComptePage, monCompteHttp, monCompteSimule } from '@gimac/compte'
import { Fournisseurs } from './Fournisseurs'
import { creerClientRequetes } from './services/clientRequetes'
import { CLE_PREMIER_ACCES_DEMO, CLE_PROFIL_DEMO, NAVIGATION_CONSOLE, PROFILS_DEMO } from './navigation'
import { servicesConsoleHttp } from './services/http'
import { servicesConsoleSimules } from './services/simule'

// Console d'administration et d'audit (couche « console », D-46, D-61, D-62) : le super administrateur,
// le RSSI et chaque utilisateur configurent tout depuis la plateforme, sans ouvrir la console du
// fournisseur d'identité. Services réels par la passerelle (VITE_SERVICES=http) ou simulés pour le
// développement ; session lue sur /api/session, second facteur exigé ; premier accès avant tout écran.
const TableauDeBordPage = lazy(() => import('./pages/TableauDeBordPage'))
const ComptesPage = lazy(() => import('./pages/ComptesPage'))
const ComptePage = lazy(() => import('./pages/ComptePage'))
const CreationComptePage = lazy(() => import('./pages/CreationComptePage'))
const JournalPage = lazy(() => import('./pages/JournalPage'))
const PolitiqueSecuritePage = lazy(() => import('./pages/PolitiqueSecuritePage'))
const MessageriePage = lazy(() => import('./pages/MessageriePage'))
const ProfilsPage = lazy(() => import('./pages/ProfilsPage'))
const ConfigurationPlateformePage = lazy(() => import('./pages/ConfigurationPlateformePage'))
const JournalisationPage = lazy(() => import('./pages/JournalisationPage'))

const VERSION = import.meta.env.VITE_VERSION ?? '0.1.0'

function profilDemo() {
  const demande = lirePreference(CLE_PROFIL_DEMO) ?? import.meta.env.VITE_PROFIL_DEMO
  const role = demande && PROFILS_DEMO[demande] ? demande : 'ROLE_SUPER_ADMIN'
  return { role, ...PROFILS_DEMO[role] }
}
function sessionDemo(): Session {
  const p = profilDemo()
  return { sujet: p.sujet, nom: p.nom, identifiant: p.identifiant, roles: [p.role], permissions: p.permissions, institutionId: '', acr: '2', doubleFacteur: true }
}

/** Écran réservé à une permission : sans elle, accès refusé explicite (la passerelle refait le contrôle). */
function Autorise({ permissions, exigee, children }: { permissions: string[]; exigee: string; children: ReactNode }) {
  const { t } = useTranslation()
  return aPermission(permissions, exigee) ? <>{children}</> : <EmptyState icon="lock" title={t('console.refuse.titre')} text={t('console.refuse.texte')} />
}

function Pages({ session, correlationId }: { session: Session; correlationId?: string }) {
  const p = session.permissions
  return (
    <GardePremierAcces>
      {(assistant) => (
        <Enveloppe couche="console" navigation={assistant ? [] : NAVIGATION_CONSOLE} permissions={p} utilisateur={session.nom} version={VERSION}
          correlationId={correlationId} espacePersonnel="/console">
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
                  <Route path="messagerie" element={<Autorise permissions={p} exigee="PLATFORM_MESSAGING_EDIT"><MessageriePage /></Autorise>} />
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

function AvecSession({ reels, children }: { reels: boolean; children: (s: Session) => ReactNode }) {
  if (!reels) {
    const s = sessionDemo()
    return <SessionProvider session={s}>{children(s)}</SessionProvider>
  }
  return <GardeSession>{children}</GardeSession>
}

export default function App() {
  const reels = import.meta.env.VITE_SERVICES === 'http'
  const services = useMemo(() => {
    if (reels) return servicesConsoleHttp()
    const p = profilDemo()
    return servicesConsoleSimules({ acteur: { id: p.sujet, nom: p.nom, permissions: p.permissions } })
  }, [reels])
  const compte = useMemo(() => {
    if (reels) return monCompteHttp('console')
    const p = profilDemo()
    const [prenom, ...nom] = p.nom.split(' ')
    return monCompteSimule({
      premierAcces: lirePreference(CLE_PREMIER_ACCES_DEMO) === '1',
      compte: { id: p.sujet, identifiant: p.identifiant, prenom, nom: nom.join(' '), nomAffiche: p.nom, email: `${p.identifiant}@gimac-afr.org`, profils: [p.role] },
    })
  }, [reels])
  const client = useMemo(() => creerClientRequetes(), [])
  const [correlationId, setCorrelationId] = useState<string | undefined>()
  useEffect(() => {
    // Session expirée : retour à la connexion (Keycloak exige de nouveau le second facteur).
    configureHttp({
      onCorrelation: setCorrelationId,
      onUnauthorized: async () => {
        if (reels) window.location.assign(CONNEXION)
        return false
      },
    })
  }, [reels])
  return (
    <Fournisseurs services={services} compte={compte} clientRequetes={client}>
      <Routes>
        <Route path="/connexion" element={<PageConnexion couche="console" />} />
        <Route path="/console/*" element={<AvecSession reels={reels}>{(s) => <Pages session={s} correlationId={correlationId} />}</AvecSession>} />
        <Route path="*" element={<Navigate to="/console/tableau-de-bord" replace />} />
      </Routes>
    </Fournisseurs>
  )
}
