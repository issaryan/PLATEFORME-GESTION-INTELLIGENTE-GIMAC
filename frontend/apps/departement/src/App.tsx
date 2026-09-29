import { lazy, Suspense, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { Skeleton } from '@gimac/design-system'
import { CONNEXION, Enveloppe, GardeSession, PageConnexion, PageDeconnexion, SessionProvider, configureHttp, lirePreference, type EtatCanal, type Session } from '@gimac/socle'
import { CompteProvider, GardePremierAcces, MonComptePage, monCompteHttp, monCompteSimule } from '@gimac/compte'
import { Fournisseurs, NAVIGATION_DES, PROFILS, creerClientRequetes, servicesHttp, servicesSimules } from '@gimac/lot1'

// Application du département (couche « des ») : routes du lot 1 (DS-GIMAC-01 §10.1). Services réels par
// la passerelle (VITE_SERVICES=http) ou simulés pour le développement des écrans.
// D-61 : la session (matrice des rôles du jeton obtenu par authentification à deux facteurs) est lue
// sur /api/session ; sans second facteur, aucun écran n'est servi. Chaque acteur voit son tableau de
// bord et les seules actions que ses permissions ouvrent ; la passerelle et les services refont le contrôle.
const TableauDeBordPage = lazy(() => import('@gimac/lot1/pages/des/TableauDeBordPage'))
const InstitutionsPage = lazy(() => import('@gimac/lot1/pages/des/InstitutionsPage'))
const InstitutionPage = lazy(() => import('@gimac/lot1/pages/des/InstitutionPage'))
const LiaisonsPage = lazy(() => import('@gimac/lot1/pages/des/LiaisonsPage'))
const LiaisonPage = lazy(() => import('@gimac/lot1/pages/des/LiaisonPage'))
const ClotureOperationPage = lazy(() => import('@gimac/lot1/pages/des/ClotureOperationPage'))
const DossierMiseEnServicePage = lazy(() => import('@gimac/lot1/pages/des/DossierMiseEnServicePage'))
const ReferentielsOperationsPage = lazy(() => import('@gimac/lot1/pages/des/ReferentielsOperationsPage'))

const REELS = import.meta.env.VITE_SERVICES === 'http'
const VERSION = import.meta.env.VITE_VERSION ?? '0.1.0'

// Session de démonstration (services simulés seulement) : profil choisi par ?profil=ROLE_… (retenu pour
// l'onglet), sinon VITE_PROFIL_DEMO. Les quinze profils par fonction (D-58) viennent de @gimac/lot1.
const profilDes = (r: string | null | undefined): r is string => Boolean(r && PROFILS[r] && r !== 'ROLE_CORRESPONDANT')
function profilDemo(): string {
  const demande = new URLSearchParams(window.location.search).get('profil')
  try {
    if (profilDes(demande)) sessionStorage.setItem('gimac.profilDemo', demande)
    const retenu = sessionStorage.getItem('gimac.profilDemo')
    if (profilDes(retenu)) return retenu
  } catch {
    if (profilDes(demande)) return demande
  }
  const env = import.meta.env.VITE_PROFIL_DEMO
  return profilDes(env) ? env : 'ROLE_CHEF_DDRM'
}
function sessionDemo(): Session {
  const role = profilDemo()
  return { sujet: 'demo', nom: 'Démonstration', identifiant: 'demo', roles: [role], permissions: PROFILS[role], institutionId: '', acr: '2', doubleFacteur: true }
}

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

function AvecSession({ children }: { children: (s: Session) => ReactNode }) {
  if (!REELS) {
    const s = sessionDemo()
    return <SessionProvider session={s}>{children(s)}</SessionProvider>
  }
  return <GardeSession>{children}</GardeSession>
}

export default function App() {
  const services = useMemo(() => (REELS ? servicesHttp('des') : servicesSimules()), [])
  // « Mon compte » (D-62) : /bff/des/iam/moi, ou service simulé (gm.premierAccesDemo = 1 : démonstration du premier accès).
  const compte = useMemo(() => (REELS ? monCompteHttp('des') : monCompteSimule({ premierAcces: lirePreference('gm.premierAccesDemo') === '1' })), [])
  const client = useMemo(() => creerClientRequetes(), [])
  const [correlationId, setCorrelationId] = useState<string | undefined>()
  useEffect(() => {
    // Session expirée : retour à la connexion (Keycloak exige de nouveau le second facteur).
    configureHttp({
      onCorrelation: setCorrelationId,
      onUnauthorized: async () => {
        if (REELS) window.location.assign(CONNEXION)
        return false
      },
    })
  }, [])
  return (
    <Fournisseurs couche="des" services={services} canal={REELS} clientRequetes={client}>
      {(etatCanal) => (
        <CompteProvider service={compte}>
          <Routes>
            <Route path="/connexion" element={<PageConnexion couche="des" />} />
            <Route path="/des/*" element={<AvecSession>{(s) => <Pages session={s} etatCanal={etatCanal} correlationId={correlationId} />}</AvecSession>} />
            <Route path="*" element={<Navigate to="/des/tableau-de-bord" replace />} />
          </Routes>
        </CompteProvider>
      )}
    </Fournisseurs>
  )
}
