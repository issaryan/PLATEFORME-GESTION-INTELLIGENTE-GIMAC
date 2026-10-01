import { lazy, Suspense, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router'
import { Skeleton } from '@gimac/design-system'
import {
  ConnexionProvider, Enveloppe, GardeSession, PageConnexion, PageDeconnexion, SessionActive, adresseConnexion, configureHttp, connexionHttp, lirePreference, lireSessionDemo,
  surNonAutorise, type EtatCanal, type Session,
} from '@gimac/socle'
import { CompteProvider, GardePremierAcces, MonComptePage, monCompteHttp, monCompteSimule } from '@gimac/compte'
import { Fournisseurs, NAVIGATION_DES, PERM, creerClientRequetes, servicesHttp, servicesSimules, useNonLus } from '@gimac/lot1'
import { PROFILS_DEMO, roleDemo, sessionDemo } from './profils'
import { connexionDemo } from './connexionDemo'

// Application du département (couche « des ») : routes du lot 1 (DS-GIMAC-01 §10.1). Services réels par
// la passerelle (VITE_SERVICES=http) ou simulés pour le développement des écrans.
// D-61 : la session (matrice des rôles du jeton obtenu par authentification à deux facteurs) est lue
// sur /api/session ; sans second facteur, aucun écran n'est servi. Chaque acteur voit son tableau de
// bord et les seules actions que ses permissions ouvrent ; la passerelle et les services refont le contrôle.
// En services simulés, la session est celle d'un profil de démonstration (profils.ts, D-57, D-58), ouverte
// par la connexion simulée (connexionDemo.ts, D-65). Connexion sur la plateforme : /connexion (D-65).
// Session fermée après 30 minutes d'inactivité (SessionActive, D-65 §6).
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
const MessageriePage = lazy(() => import('@gimac/lot1/pages/des/messagerie/MessageriePage'))

const VERSION = import.meta.env.VITE_VERSION ?? '0.1.0'

// D-62 : après la garde de session, le premier accès (fiche, charte, actions requises) passe avant
// tout tableau de bord ; « Mon compte » et la déconnexion sont dans l'espace personnel de l'enveloppe.
function Pages({ session, etatCanal, correlationId }: { session: Session; etatCanal: EtatCanal | undefined; correlationId?: string }) {
  // Messages non lus (D-64) : pastille de l'entrée « Messagerie », rafraîchie par le canal poussé.
  const nonLus = useNonLus(session.permissions.includes(PERM.messageLire))
  return (
    <GardePremierAcces>
      {(assistant) => (
        <Enveloppe
          couche="des"
          navigation={assistant ? [] : NAVIGATION_DES}
          permissions={session.permissions}
          badges={{ messagerie: nonLus.data?.total ?? 0 }}
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
                  <Route path="messagerie" element={<MessageriePage />} />
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
  const ici = useLocation()
  if (!reels) {
    if (!lireSessionDemo()) return <Navigate to={adresseConnexion({ retour: ici.pathname + ici.search })} replace />
    const s = sessionDemo()
    return <SessionActive session={s} prolonger={async () => undefined}>{children(s)}</SessionActive>
  }
  return <GardeSession>{children}</GardeSession>
}

export default function App() {
  const reels = import.meta.env.VITE_SERVICES === 'http'
  const services = useMemo(() => {
    if (reels) return servicesHttp('des')
    const s = sessionDemo()
    return servicesSimules({ utilisateur: { id: s.sujet, nom: s.nom } })
  }, [reels])
  // « Mon compte » (D-62) : /bff/des/iam/moi, ou service simulé au nom du profil de démonstration
  // (gm.premierAccesDemo = 1 : démonstration du premier accès).
  const compte = useMemo(() => {
    if (reels) return monCompteHttp('des')
    const role = roleDemo()
    const p = PROFILS_DEMO[role]
    const demo = lireSessionDemo()
    const identifiant = demo?.identifiant || p.identifiant
    const [prenom, ...nom] = p.nom.split(' ')
    return monCompteSimule({
      premierAcces: demo?.premierAcces ?? lirePreference('gm.premierAccesDemo') === '1',
      etapes: demo?.etapes,
      compte: { id: p.sujet, identifiant, prenom, nom: nom.join(' '), nomAffiche: p.nom, email: `${identifiant}@gimac-afr.org`, profils: [role] },
    })
  }, [reels])
  const connexion = useMemo(() => (reels ? connexionHttp() : connexionDemo()), [reels])
  const client = useMemo(() => creerClientRequetes(), [])
  const [correlationId, setCorrelationId] = useState<string | undefined>()
  useEffect(() => {
    // Session absente ou close par la passerelle (401, CNX_SESSION_EXPIREE) : retour à la connexion.
    configureHttp({
      onCorrelation: setCorrelationId,
      onUnauthorized: async (e) => (reels ? surNonAutorise(e) : false),
    })
  }, [reels])
  return (
    <Fournisseurs couche="des" services={services} canal={reels} clientRequetes={client}>
      {(etatCanal) => (
        <ConnexionProvider service={connexion} couche="des">
        <CompteProvider service={compte}>
          <Routes>
            <Route path="/connexion/*" element={<PageConnexion couche="des" />} />
            <Route path="/des/*" element={<AvecSession reels={reels}>{(s) => <Pages session={s} etatCanal={etatCanal} correlationId={correlationId} />}</AvecSession>} />
            <Route path="*" element={<Navigate to="/des/tableau-de-bord" replace />} />
          </Routes>
        </CompteProvider>
        </ConnexionProvider>
      )}
    </Fournisseurs>
  )
}
