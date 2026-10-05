import { lazy, Suspense, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router'
import { Skeleton } from '@gimac/design-system'
import {
  Aiguillage, ConnexionProvider, Enveloppe, GardeSession, PageConnexion, PageDeconnexion, SessionActive, adresseConnexion, configureHttp, connexionHttp, lirePreference, lireSessionDemo,
  surNonAutorise, type EtatCanal, type Session,
} from '@gimac/socle'
import { CompteProvider, GardePremierAcces, MonComptePage, useMonAvatar, monCompteHttp, monCompteSimule } from '@gimac/compte'
import { Fournisseurs, NAVIGATION_PORTAIL, creerClientRequetes, servicesHttp, servicesSimules } from '@gimac/lot1'
import { connexionDemo, sessionCorrespondant } from './connexionDemo'

// Portail des participants (couche « portail ») : POR-03 du lot 1 (DS-GIMAC-01 §10.2), tableau de bord du
// participant et gestes qui lui incombent (qualité Externe, CR §3.4, D-56 à D-59). Toutes les données
// sont cloisonnées à l'institution du jeton par la passerelle et les services.
// D-61, D-62 : session lue sur /api/session (second facteur exigé), premier accès avant tout écran,
// « Mon compte » et déconnexion dans l'espace personnel de l'enveloppe. D-65 : connexion sur la plateforme
// (/connexion, connexion simulée en démonstration : connexionDemo.ts), fermeture après 30 minutes d'inactivité.
const TableauDeBordPage = lazy(() => import('@gimac/lot1/pages/portail/parcours/TableauDeBordPage'))
const MaLiaisonPage = lazy(() => import('@gimac/lot1/pages/portail/MaLiaisonPage'))
const DepotPreuvePage = lazy(() => import('@gimac/lot1/pages/portail/DepotPreuvePage'))
const CoordonneesPage = lazy(() => import('@gimac/lot1/pages/portail/parcours/CoordonneesPage'))
const ProcesVerbalPage = lazy(() => import('@gimac/lot1/pages/portail/parcours/ProcesVerbalPage'))
const AttestationsPage = lazy(() => import('@gimac/lot1/pages/portail/parcours/AttestationsPage'))
const FicheSuiviPage = lazy(() => import('@gimac/lot1/pages/portail/parcours/FicheSuiviPage'))

const REELS = import.meta.env.VITE_SERVICES !== 'simule'
const VERSION = import.meta.env.VITE_VERSION ?? '0.1.0'

function sessionDemo(): Session {
  const demo = lireSessionDemo()
  return { ...sessionCorrespondant(demo?.identifiant || undefined), ...(demo?.premierAcces ? { restreinte: true } : {}) }
}

function AvecSession({ children }: { children: (s: Session) => ReactNode }) {
  const ici = useLocation()
  if (!REELS) {
    if (!lireSessionDemo()) return <Navigate to={adresseConnexion({ retour: ici.pathname + ici.search })} replace />
    const s = sessionDemo()
    return <SessionActive session={s} prolonger={async () => undefined}><Aiguillage session={s} couche="portail">{children(s)}</Aiguillage></SessionActive>
  }
  // Aiguillage (D-79) : un compte interne (session sans institution) est conduit vers l'application interne.
  return <GardeSession>{(s) => <Aiguillage session={s} couche="portail">{children(s)}</Aiguillage>}</GardeSession>
}

function Pages({ session, etatCanal, correlationId }: { session: Session; etatCanal: EtatCanal | undefined; correlationId?: string }) {
  // Photo de profil du titulaire (docs/lot1/mon-compte.md §4) : menu utilisateur de la barre du haut.
  const moi = useMonAvatar()
  return (
    <GardePremierAcces>
      {(assistant) => (
        <Enveloppe avatar={moi?.src} fonction={moi?.fonction} couche="portail" navigation={assistant ? [] : NAVIGATION_PORTAIL} permissions={session.permissions} utilisateur={session.nom}
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
  const compte = useMemo(() => {
    if (REELS) return monCompteHttp('portail')
    const demo = lireSessionDemo()
    return monCompteSimule({
      couche: 'portail',
      premierAcces: demo?.premierAcces ?? lirePreference('gm.premierAccesDemo') === '1',
      etapes: demo?.etapes,
      compte: { identifiant: demo?.identifiant || 'correspondant-a', prenom: 'Brice', nom: 'Nzeng Mba', nomAffiche: 'Brice Nzeng Mba', profils: ['ROLE_CORRESPONDANT'], institutionId: 'INST-0042' },
    })
  }, [])
  const connexion = useMemo(() => (REELS ? connexionHttp() : connexionDemo()), [])
  const client = useMemo(() => creerClientRequetes(), [])
  const [correlationId, setCorrelationId] = useState<string | undefined>()
  useEffect(() => {
    // Session absente ou close par la passerelle (401, CNX_SESSION_EXPIREE) : retour à la connexion (D-65).
    configureHttp({
      onCorrelation: setCorrelationId,
      onUnauthorized: async (e) => (REELS ? surNonAutorise(e) : false),
    })
  }, [])
  return (
    <Fournisseurs couche="portail" services={services} canal={REELS} clientRequetes={client}>
      {(etatCanal) => (
        <ConnexionProvider service={connexion} couche="portail">
        <CompteProvider service={compte}>
          <Routes>
            <Route path="/connexion/*" element={<PageConnexion couche="portail" />} />
            <Route path="/portail/*" element={<AvecSession>{(s) => <Pages session={s} etatCanal={etatCanal} correlationId={correlationId} />}</AvecSession>} />
            <Route path="*" element={<Navigate to="/portail/tableau-de-bord" replace />} />
          </Routes>
        </CompteProvider>
        </ConnexionProvider>
      )}
    </Fournisseurs>
  )
}
