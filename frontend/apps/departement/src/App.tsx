import { lazy, Suspense, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { Skeleton } from '@gimac/design-system'
import { CONNEXION, Enveloppe, GardeSession, PageConnexion, PageDeconnexion, SessionProvider, configureHttp, lirePreference, type EtatCanal, type Session } from '@gimac/socle'
import { CompteProvider, GardePremierAcces, MonComptePage, monCompteHttp, monCompteSimule } from '@gimac/compte'
import { Fournisseurs, NAVIGATION_DES, creerClientRequetes, servicesHttp, servicesSimules } from '@gimac/lot1'
import { PROFILS_DEMO, roleDemo, sessionDemo } from './profils'

// Application du département (couche « des ») : routes du lot 1 (DS-GIMAC-01 §10.1). Services réels par
// la passerelle (VITE_SERVICES=http) ou simulés pour le développement des écrans.
// D-61 : la session (matrice des rôles du jeton obtenu par authentification à deux facteurs) est lue
// sur /api/session ; sans second facteur, aucun écran n'est servi. Chaque acteur voit son tableau de
// bord et les seules actions que ses permissions ouvrent ; la passerelle et les services refont le contrôle.
// En services simulés, la session est celle d'un profil de démonstration (profils.ts, D-57, D-58).
const TableauDeBordPage = lazy(() => import('@gimac/lot1/pages/des/TableauDeBordPage'))
const InstitutionsPage = lazy(() => import('@gimac/lot1/pages/des/InstitutionsPage'))
const InstitutionPage = lazy(() => import('@gimac/lot1/pages/des/InstitutionPage'))
const LiaisonsPage = lazy(() => import('@gimac/lot1/pages/des/LiaisonsPage'))
const LiaisonPage = lazy(() => import('@gimac/lot1/pages/des/LiaisonPage'))
const ClotureOperationPage = lazy(() => import('@gimac/lot1/pages/des/ClotureOperationPage'))
const DossierMiseEnServicePage = lazy(() => import('@gimac/lot1/pages/des/DossierMiseEnServicePage'))
const ParcoursPage = lazy(() => import('@gimac/lot1/pages/des/parcours/ParcoursPage'))
const DossierLiaisonPage = lazy(() => import('@gimac/lot1/pages/des/parcours/DossierLiaisonPage'))
const ReferentielsOperationsPage = lazy(() => import('@gimac/lot1/pages/des/ReferentielsOperationsPage'))

const VERSION = import.meta.env.VITE_VERSION ?? '0.1.0'

// D-62 : après la garde de session, le premier accès (fiche, charte, actions requises) passe avant
// tout tableau de bord ; « Mon compte » et la déconnexion sont dans l'espace personnel de l'enveloppe.
function Pages({ session, etatCanal, correlationId }: { session: Session; etatCanal: EtatCanal | undefined; correlationId?: string }) {
  return (
    <GardePremierAcces>
      {(assistant) => (
        <Enveloppe
          couche="des"
          navigation={assistant ? [] : NAVIGATION_DES}
          permissions={session.permissions}
          utilisateur={session.nom}
          etatCanal={etatCanal}
          version={VERSION}
          correlationId={correlationId}
          espacePersonnel="/des"
        >
          <Suspense fallback={<Skeleton height={32} />}>
            <Routes>
              <Route path="deconnexion" element={<PageDeconnexion />} />
              {assistant ? (
                <Route path="*" element={assistant} />
              ) : (
                <>
                  <Route path="mon-compte" element={<MonComptePage />} />
                  <Route path="tableau-de-bord" element={<TableauDeBordPage />} />
                  <Route path="institutions" element={<InstitutionsPage />} />
                  <Route path="institutions/:id" element={<InstitutionPage />} />
                  <Route path="liaisons" element={<LiaisonsPage />} />
                  <Route path="liaisons/:id" element={<LiaisonPage />} />
                  <Route path="liaisons/:id/operations/:op/cloture" element={<ClotureOperationPage />} />
                  <Route path="liaisons/:id/mise-en-service" element={<DossierMiseEnServicePage />} />
                  <Route path="parcours" element={<ParcoursPage />} />
                  <Route path="parcours/:id" element={<DossierLiaisonPage />} />
                  <Route path="referentiels/operations" element={<ReferentielsOperationsPage />} />
                  <Route path="*" element={<Navigate to="/des/tableau-de-bord" replace />} />
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
  const services = useMemo(() => (reels ? servicesHttp('des') : servicesSimules()), [reels])
  // « Mon compte » (D-62) : /bff/des/iam/moi, ou service simulé au nom du profil de démonstration
  // (gm.premierAccesDemo = 1 : démonstration du premier accès).
  const compte = useMemo(() => {
    if (reels) return monCompteHttp('des')
    const role = roleDemo()
    const p = PROFILS_DEMO[role]
    const [prenom, ...nom] = p.nom.split(' ')
    return monCompteSimule({
      premierAcces: lirePreference('gm.premierAccesDemo') === '1',
      compte: { id: p.sujet, identifiant: p.identifiant, prenom, nom: nom.join(' '), nomAffiche: p.nom, email: `${p.identifiant}@gimac-afr.org`, profils: [role] },
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
    <Fournisseurs couche="des" services={services} canal={reels} clientRequetes={client}>
      {(etatCanal) => (
        <CompteProvider service={compte}>
          <Routes>
            <Route path="/connexion" element={<PageConnexion couche="des" />} />
            <Route path="/des/*" element={<AvecSession reels={reels}>{(s) => <Pages session={s} etatCanal={etatCanal} correlationId={correlationId} />}</AvecSession>} />
            <Route path="*" element={<Navigate to="/des/tableau-de-bord" replace />} />
          </Routes>
        </CompteProvider>
      )}
    </Fournisseurs>
  )
}
