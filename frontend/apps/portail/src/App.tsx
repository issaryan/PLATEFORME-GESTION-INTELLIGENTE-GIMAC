import { lazy, Suspense, useMemo } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { Skeleton } from '@gimac/design-system'
import { Enveloppe, PageConnexion } from '@gimac/socle'
import { Fournisseurs, NAVIGATION_PORTAIL, creerClientRequetes, servicesHttp, servicesSimules } from '@gimac/lot1'

// Portail des participants (couche « portail ») : POR-03 du lot 1 (DS-GIMAC-01 §10.2). Toutes les données
// sont cloisonnées à l'institution du jeton par la passerelle et les services.
const MaLiaisonPage = lazy(() => import('@gimac/lot1/pages/portail/MaLiaisonPage'))
const DepotPreuvePage = lazy(() => import('@gimac/lot1/pages/portail/DepotPreuvePage'))

const REELS = import.meta.env.VITE_SERVICES === 'http'
const PERMISSIONS_DEMO = ['HOM_PORTAL_READ', 'HOM_PORTAL_SUBMIT', 'CER_PORTAL_READ']

export default function App() {
  const services = useMemo(() => (REELS ? servicesHttp('portail') : servicesSimules()), [])
  const client = useMemo(creerClientRequetes, [])
  return (
    <Fournisseurs couche="portail" services={services} canal={REELS} clientRequetes={client}>
      {(etatCanal) => (
        <Routes>
          <Route path="/connexion" element={<PageConnexion couche="portail" />} />
          <Route
            path="/portail/*"
            element={
              <Enveloppe couche="portail" navigation={NAVIGATION_PORTAIL} permissions={PERMISSIONS_DEMO} utilisateur="Correspondant A" institution="Établissement de paiement A" etatCanal={etatCanal}>
                <Suspense fallback={<Skeleton height={32} />}>
                  <Routes>
                    <Route path="liaisons" element={<MaLiaisonPage />} />
                    <Route path="liaisons/:id" element={<MaLiaisonPage />} />
                    <Route path="liaisons/:id/operations/:op/preuve" element={<DepotPreuvePage />} />
                    <Route path="*" element={<Navigate to="/portail/liaisons" replace />} />
                  </Routes>
                </Suspense>
              </Enveloppe>
            }
          />
          <Route path="*" element={<Navigate to="/portail/liaisons" replace />} />
        </Routes>
      )}
    </Fournisseurs>
  )
}
