import { lazy, Suspense, useMemo } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { Skeleton } from '@gimac/design-system'
import { Enveloppe, PageConnexion } from '@gimac/socle'
import { Fournisseurs, NAVIGATION_DES, creerClientRequetes, servicesHttp, servicesSimules } from '@gimac/lot1'

// Application du département (couche « des ») : routes du lot 1 (DS-GIMAC-01 §10.1). Les services sont
// simulés tant que la passerelle n'expose pas les routes /bff du lot (VITE_SERVICES=http pour les réels).
const LiaisonsPage = lazy(() => import('@gimac/lot1/pages/des/LiaisonsPage'))
const LiaisonPage = lazy(() => import('@gimac/lot1/pages/des/LiaisonPage'))
const ClotureOperationPage = lazy(() => import('@gimac/lot1/pages/des/ClotureOperationPage'))
const DossierMiseEnServicePage = lazy(() => import('@gimac/lot1/pages/des/DossierMiseEnServicePage'))
const ReferentielsOperationsPage = lazy(() => import('@gimac/lot1/pages/des/ReferentielsOperationsPage'))

const REELS = import.meta.env.VITE_SERVICES === 'http'
// Permissions du profil de démonstration (ingénieur d'homologation et chef du DES) ; en réel, lues du jeton.
const PERMISSIONS_DEMO = ['HOM_LIAISON_LIST', 'HOM_LIAISON_READ', 'HOM_LIAISON_OPEN', 'HOM_LIAISON_UNFREEZE', 'HOM_DASHBOARD_READ', 'HOM_REFERENTIEL_READ',
  'HOM_REFERENTIEL_EDIT', 'HOM_REFERENTIEL_PUBLISH', 'HOM_REFERENTIEL_MIGRATE', 'HOM_OPERATION_ASSIGN', 'HOM_EVIDENCE_SUBMIT', 'HOM_OPERATION_VALIDATE',
  'HOM_OPERATION_REOPEN', 'HOM_GRID_EDIT', 'HOM_SCENARIO_QUALIFY', 'HOM_EVIDENCE_FILE_EXPORT', 'CER_DOSSIER_READ', 'CER_DOSSIER_VERIFY',
  'CER_CHANGE_RECORD_LINK', 'CER_GOLIVE_REPORT_SUBMIT', 'CER_ATTESTATION_ISSUE', 'CER_QUITUS_SECURITE_SIGN', 'CER_REGISTER_READ', 'CER_REGISTER_EXPORT']

export default function App() {
  const services = useMemo(() => (REELS ? servicesHttp('des') : servicesSimules()), [])
  const client = useMemo(creerClientRequetes, [])
  return (
    <Fournisseurs couche="des" services={services} canal={REELS} clientRequetes={client}>
      {(etatCanal) => (
        <Routes>
          <Route path="/connexion" element={<PageConnexion couche="des" />} />
          <Route
            path="/des/*"
            element={
              <Enveloppe couche="des" navigation={NAVIGATION_DES} permissions={PERMISSIONS_DEMO} utilisateur="Ingénieur d'homologation" etatCanal={etatCanal}>
                <Suspense fallback={<Skeleton height={32} />}>
                  <Routes>
                    <Route path="liaisons" element={<LiaisonsPage />} />
                    <Route path="liaisons/:id" element={<LiaisonPage />} />
                    <Route path="liaisons/:id/operations/:op/cloture" element={<ClotureOperationPage />} />
                    <Route path="liaisons/:id/mise-en-service" element={<DossierMiseEnServicePage />} />
                    <Route path="referentiels/operations" element={<ReferentielsOperationsPage />} />
                    <Route path="*" element={<Navigate to="/des/liaisons" replace />} />
                  </Routes>
                </Suspense>
              </Enveloppe>
            }
          />
          <Route path="*" element={<Navigate to="/des/liaisons" replace />} />
        </Routes>
      )}
    </Fournisseurs>
  )
}
