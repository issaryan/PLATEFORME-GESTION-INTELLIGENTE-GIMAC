import { lazy, Suspense, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { Skeleton } from '@gimac/design-system'
import { CONNEXION, Enveloppe, GardeSession, PageConnexion, PageDeconnexion, SessionProvider, configureHttp, lirePreference, type EtatCanal, type Session } from '@gimac/socle'
import { CompteProvider, GardePremierAcces, MonComptePage, monCompteHttp, monCompteSimule } from '@gimac/compte'
import { Fournisseurs, NAVIGATION_PORTAIL, PROFILS, creerClientRequetes, servicesHttp, servicesSimules } from '@gimac/lot1'

// Portail des participants (couche « portail ») : POR-03 du lot 1 (DS-GIMAC-01 §10.2), tableau de bord du
// participant et gestes qui lui incombent (qualité Externe, CR §3.4, D-56 à D-59). Toutes les données
// sont cloisonnées à l'institution du jeton par la passerelle et les services.
// D-61, D-62 : session lue sur /api/session (second facteur exigé), premier accès avant tout écran,
// « Mon compte » et déconnexion dans l'espace personnel de l'enveloppe.
const TableauDeBordPage = lazy(() => import('@gimac/lot1/pages/portail/parcours/TableauDeBordPage'))
const MaLiaisonPage = lazy(() => import('@gimac/lot1/pages/portail/MaLiaisonPage'))
const DepotPreuvePage = lazy(() => import('@gimac/lot1/pages/portail/DepotPreuvePage'))
const CoordonneesPage = lazy(() => import('@gimac/lot1/pages/portail/parcours/CoordonneesPage'))
const ProcesVerbalPage = lazy(() => import('@gimac/lot1/pages/portail/parcours/ProcesVerbalPage'))
const AttestationsPage = lazy(() => import('@gimac/lot1/pages/portail/parcours/AttestationsPage'))
const FicheSuiviPage = lazy(() => import('@gimac/lot1/pages/portail/parcours/FicheSuiviPage'))

const REELS = import.meta.env.VITE_SERVICES === 'http'
const VERSION = import.meta.env.VITE_VERSION ?? '0.1.0'

function sessionDemo(): Session {
  return { sujet: 'demo-correspondant', nom: 'Correspondant A', identifiant: 'correspondant-a', roles: ['ROLE_CORRESPONDANT'], permissions: PROFILS.ROLE_CORRESPONDANT, institutionId: 'INST-0042', acr: '2', doubleFacteur: true }
}

function AvecSession({ children }: { children: (s: Session) => ReactNode }) {
  if (!REELS) {
    const s = sessionDemo()
    return <SessionProvider session={s}>{children(s)}</SessionProvider>
  }
  return <GardeSession>{children}</GardeSession>
}

function Pages({ session, etatCanal, correlationId }: { session: Session; etatCanal: EtatCanal | undefined; correlationId?: string }) {
  return (
    <GardePremierAcces>
      {(assistant) => (
        <Enveloppe couche="portail" navigation={assistant ? [] : NAVIGATION_PORTAIL} permissions={session.permissions} utilisateur={session.nom}
          institution={REELS ? undefined : 'Établissement de paiement A'} etatCanal={etatCanal} version={VERSION} correlationId={correlationId} espacePersonnel="/portail">
          <Suspense fallback={<Skeleton height={32} />}>
            <Routes>
              <Route path="deconnexion" element={<PageDeconnexion />} />
              {assistant ? (
                <Route path="*" element={assistant} />
              ) : (
                <>
                  <Route path="mon-compte" element={<MonComptePage />} />
                  <Route path="tableau-de-bord" element={<TableauDeBordPage />} />
                  <Route path="liaisons" element={<MaLiaisonPage />} />
                  <Route path="liaisons/:id" element={<MaLiaisonPage />} />
                  <Route path="liaisons/:id/coordonnees" element={<CoordonneesPage />} />
                  <Route path="liaisons/:id/livrables/:op" element={<DepotPreuvePage />} />
                  <Route path="liaisons/:id/proces-verbal" element={<ProcesVerbalPage />} />
                  <Route path="liaisons/:id/attestations" element={<AttestationsPage />} />
                  <Route path="liaisons/:id/fiche-suivi" element={<FicheSuiviPage />} />
                  <Route path="*" element={<Navigate to="/portail/tableau-de-bord" replace />} />
                </>
              )}
            </Routes>
          </Suspense>
        </Enveloppe>
      )}
    </GardePremierAcces>
  )
}

export default function App() {
  const services = useMemo(() => (REELS ? servicesHttp('portail') : servicesSimules()), [])
  const compte = useMemo(
    () => (REELS ? monCompteHttp('portail') : monCompteSimule({ premierAcces: lirePreference('gm.premierAccesDemo') === '1', compte: { identifiant: 'correspondant-a', prenom: 'Brice', nom: 'Nzeng Mba', nomAffiche: 'Brice Nzeng Mba', profils: ['ROLE_CORRESPONDANT'], institutionId: 'INST-0042' } })),
    [],
  )
  const client = useMemo(() => creerClientRequetes(), [])
  const [correlationId, setCorrelationId] = useState<string | undefined>()
  useEffect(() => {
    // Session expirée : retour à la connexion (le fournisseur d'identité exige de nouveau le second facteur).
    configureHttp({
      onCorrelation: setCorrelationId,
      onUnauthorized: async () => {
        if (REELS) window.location.assign(CONNEXION)
        return false
      },
    })
  }, [])
  return (
    <Fournisseurs couche="portail" services={services} canal={REELS} clientRequetes={client}>
      {(etatCanal) => (
        <CompteProvider service={compte}>
          <Routes>
            <Route path="/connexion" element={<PageConnexion couche="portail" />} />
            <Route path="/portail/*" element={<AvecSession>{(s) => <Pages session={s} etatCanal={etatCanal} correlationId={correlationId} />}</AvecSession>} />
            <Route path="*" element={<Navigate to="/portail/tableau-de-bord" replace />} />
          </Routes>
        </CompteProvider>
      )}
    </Fournisseurs>
  )
}
