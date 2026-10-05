import { lazy, Suspense, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router'
import { EmptyState, Skeleton } from '@gimac/design-system'
import { useTranslation } from 'react-i18next'
import {
  Aiguillage, ConnexionProvider, Enveloppe, aPermission, GardeSession, NotificationsPage, NotificationsProvider, PageConnexion, PageDeconnexion, SessionActive, adresseConnexion, configureHttp,
  connexionHttp, lirePreference, lireSessionDemo, notificationsHttp, surNonAutorise, type EtatCanal, type LecteurApercu, type ServiceNotifications, type Session,
} from '@gimac/socle'
import { CompteProvider, GardePremierAcces, MonComptePage, useMonAvatar, monCompteHttp, monCompteSimule } from '@gimac/compte'
import {
  ExtensionsTableauDeBordProvider, Fournisseurs, NAVIGATION_DES, ORGANISATION_DEMO, PERM, creerClientRequetes, lecteurApercu, notificationsDemo, rattachementDemo, servicesHttp, servicesSimules, useNonLus,
  useSourcesRecherche,
} from '@gimac/lot1'
import {
  ServicesConsoleProvider, TableauAdministrationComptes, WidgetChargesEnErreur, WidgetSupervision, servicesConsoleHttp, servicesConsoleSimules, useSourceComptes,
} from '@gimac/administration'
import { PROFILS_DEMO, roleDemo, sessionDemo } from './profils'
import { connexionDemo } from './connexionDemo'

// Application du département (couche « des ») : routes du lot 1 (DS-GIMAC-01 §10.1). Services réels par
// la passerelle par défaut ; services simulés seulement sur demande explicite (VITE_SERVICES=simule : tests de bout
// en bout et démonstrations). Aucune donnée fictive n'est servie par une construction de production.
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
// Mon activité (D-76, docs/lot1/mon-activite.md) : journée, agenda, tâches, rapports et équipe de chacun.
const MaJourneePage = lazy(() => import('@gimac/lot1/pages/des/activite/MaJourneePage'))
const AgendaPage = lazy(() => import('@gimac/lot1/pages/des/activite/AgendaPage'))
const TachesPage = lazy(() => import('@gimac/lot1/pages/des/activite/TachesPage'))
const RapportsPage = lazy(() => import('@gimac/lot1/pages/des/activite/RapportsPage'))
const EquipePage = lazy(() => import('@gimac/lot1/pages/des/activite/EquipePage'))
// Administration des comptes (D-69) : écrans de @gimac/administration servis sous /des/administration,
// pour le RSSI et le super administrateur ; la console garde la supervision et la configuration.
const ComptesPage = lazy(() => import('@gimac/administration/pages/ComptesPage'))
const ComptePage = lazy(() => import('@gimac/administration/pages/ComptePage'))
const CreationComptePage = lazy(() => import('@gimac/administration/pages/CreationComptePage'))
const JournalAdministrationPage = lazy(() => import('@gimac/administration/pages/JournalPage'))
const ProfilsPage = lazy(() => import('@gimac/administration/pages/ProfilsPage'))
const PolitiqueSecuritePage = lazy(() => import('@gimac/administration/pages/PolitiqueSecuritePage'))
const OrganisationPage = lazy(() => import('@gimac/administration/pages/OrganisationPage'))
// Plateforme (D-79) : les écrans de la console servis aussi dans l'application interne, sous /des/plateforme ;
// /api/plateforme est servi par la passerelle depuis toute couche (flux SSE compris).
const SupervisionPage = lazy(() => import('@gimac/administration/pages/SupervisionPage'))
const InfrastructurePage = lazy(() => import('@gimac/administration/pages/InfrastructurePage'))
const ConfigurationPlateformePage = lazy(() => import('@gimac/administration/pages/ConfigurationPlateformePage'))
const JournalisationPage = lazy(() => import('@gimac/administration/pages/JournalisationPage'))
const CourrielsPage = lazy(() => import('@gimac/administration/pages/MessageriePage'))
const CHEMINS_ADMINISTRATION = { base: '/des/administration', espace: '/des', plateforme: '/des/plateforme' }
// Widgets du tableau de bord fournis par l'administration : indicateurs, actions et derniers actes d'administration
// des comptes (D-69), résumé de la supervision et charges de travail en erreur (D-78, D-79).
const EXTENSIONS_TABLEAU = { administration: TableauAdministrationComptes, supervision: WidgetSupervision, infrastructure: WidgetChargesEnErreur }

/** Écran réservé à une permission : sans elle, accès refusé explicite (la passerelle refait le contrôle). */
function Autorise({ permissions, exigee, children }: { permissions: string[]; exigee: string; children: ReactNode }) {
  const { t } = useTranslation()
  return aPermission(permissions, exigee) ? <>{children}</> : <EmptyState icon="lock" title={t('console.refuse.titre')} text={t('console.refuse.texte')} />
}

const VERSION = import.meta.env.VITE_VERSION ?? '0.1.0'

// D-62 : après la garde de session, le premier accès (fiche, charte, actions requises) passe avant
// tout tableau de bord ; « Mon compte » et la déconnexion sont dans l'espace personnel de l'enveloppe.
/** Notifications de la plateforme (D-80) : MS-NOTIFICATION (ou sa simulation) et l'aperçu lu auprès de la messagerie. */
type Notifications = { service: ServiceNotifications; apercu: LecteurApercu }

function Pages({ session, etatCanal, correlationId, notifications }: { session: Session; etatCanal: EtatCanal | undefined; correlationId?: string; notifications: Notifications }) {
  // Photo de profil du titulaire (docs/lot1/mon-compte.md §4) : menu utilisateur de la barre du haut.
  const moi = useMonAvatar()
  // Messages non lus (D-64) : pastille de l'entrée « Messagerie », rafraîchie par le canal poussé.
  const nonLus = useNonLus(session.permissions.includes(PERM.messageLire))
  const p = session.permissions
  // Recherche globale (Ctrl+K) : liaisons, institutions, conversations et, pour l'administration, comptes.
  const objets = useSourcesRecherche(p, '/des')
  const comptes = useSourceComptes(p)
  const recherche = useMemo(() => [...objets, ...comptes], [objets, comptes])
  return (
    <GardePremierAcces>
      {(assistant) => (
        // Cloche, fenêtres surgissantes et page des notifications (D-80) : NOT_INBOX_READ, hors premier accès.
        <NotificationsProvider service={notifications.service} couche="des" actif={!assistant && aPermission(p, PERM.notificationsLire)} apercu={notifications.apercu}>
        <Enveloppe
          avatar={moi?.src}
          fonction={moi?.fonction}
          couche="des"
          navigation={assistant ? [] : NAVIGATION_DES}
          permissions={session.permissions}
          badges={{ messagerie: nonLus.data?.total ?? 0 }}
          utilisateur={session.nom}
          etatCanal={etatCanal}
          version={VERSION}
          correlationId={correlationId}
          espacePersonnel="/des"
          recherche={assistant ? undefined : recherche}
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
                  <Route path="notifications" element={<Autorise permissions={p} exigee={PERM.notificationsLire}><NotificationsPage /></Autorise>} />
                  <Route path="activite" element={<Autorise permissions={p} exigee="PIL_ACTIVITY_OWN"><MaJourneePage /></Autorise>} />
                  <Route path="agenda" element={<Autorise permissions={p} exigee="PIL_ACTIVITY_OWN"><AgendaPage /></Autorise>} />
                  <Route path="taches" element={<Autorise permissions={p} exigee="PIL_ACTIVITY_OWN"><TachesPage /></Autorise>} />
                  <Route path="rapports" element={<Autorise permissions={p} exigee="PIL_ACTIVITY_OWN"><RapportsPage /></Autorise>} />
                  <Route path="equipe" element={<Autorise permissions={p} exigee="PIL_TEAM_READ"><EquipePage /></Autorise>} />
                  <Route path="referentiels/operations" element={<ReferentielsOperationsPage />} />
                  <Route path="administration/comptes" element={<Autorise permissions={p} exigee="IAM_ACCOUNT_READ"><ComptesPage /></Autorise>} />
                  <Route path="administration/comptes/nouveau" element={<Autorise permissions={p} exigee="IAM_ACCOUNT_CREATE"><CreationComptePage /></Autorise>} />
                  <Route path="administration/comptes/:id" element={<Autorise permissions={p} exigee="IAM_ACCOUNT_READ"><ComptePage /></Autorise>} />
                  <Route path="administration/journal" element={<Autorise permissions={p} exigee="IAM_JOURNAL_READ"><JournalAdministrationPage /></Autorise>} />
                  <Route path="administration/profils" element={<Autorise permissions={p} exigee="IAM_ACCOUNT_READ"><ProfilsPage /></Autorise>} />
                  <Route path="administration/organisation" element={<Autorise permissions={p} exigee="IAM_ACCOUNT_READ"><OrganisationPage /></Autorise>} />
                  <Route path="administration/politique-securite" element={<Autorise permissions={p} exigee="PLATFORM_SECURITY_POLICY_READ"><PolitiqueSecuritePage /></Autorise>} />
                  <Route path="plateforme/supervision" element={<Autorise permissions={p} exigee="PLATFORM_SUPERVISION_READ"><SupervisionPage /></Autorise>} />
                  <Route path="plateforme/infrastructure" element={<Autorise permissions={p} exigee="PLATFORM_OPS_READ"><InfrastructurePage /></Autorise>} />
                  <Route path="plateforme/configuration" element={<Autorise permissions={p} exigee="PLATFORM_CONFIG_READ"><ConfigurationPlateformePage /></Autorise>} />
                  <Route path="plateforme/journalisation" element={<Autorise permissions={p} exigee="PLATFORM_CONFIG_READ"><JournalisationPage /></Autorise>} />
                  <Route path="plateforme/courriels" element={<Autorise permissions={p} exigee="PLATFORM_MESSAGING_EDIT"><CourrielsPage /></Autorise>} />
                  <Route path="*" element={<Navigate to="/des/tableau-de-bord" replace />} />
                </>
              )}
            </Routes>
          </Suspense>
        </Enveloppe>
        </NotificationsProvider>
      )}
    </GardePremierAcces>
  )
}

function AvecSession({ children }: { children: (s: Session) => ReactNode }) {
  const ici = useLocation()
  // Lu ici et non reçu en propriété : constant à la construction, la branche simulée (profils et comptes de
  // démonstration) disparaît du paquet servi en production.
  if (import.meta.env.VITE_SERVICES === 'simule') {
    if (!lireSessionDemo()) return <Navigate to={adresseConnexion({ retour: ici.pathname + ici.search })} replace />
    const s = sessionDemo()
    return <SessionActive session={s} prolonger={async () => undefined}><Aiguillage session={s} couche="des">{children(s)}</Aiguillage></SessionActive>
  }
  // Aiguillage (D-79) : un compte d'institution est conduit vers le portail des participants.
  return <GardeSession>{(s) => <Aiguillage session={s} couche="des">{children(s)}</Aiguillage>}</GardeSession>
}

export default function App() {
  const reels = import.meta.env.VITE_SERVICES !== 'simule'
  const client = useMemo(() => creerClientRequetes(), [])
  // Notifications de la plateforme (D-80) : /bff/des/notification, ou MS-NOTIFICATION simulé branché sur la
  // messagerie simulée (un message reçu crée ou regroupe une notification).
  const { services, notifications } = useMemo(() => {
    if (reels) {
      const s = servicesHttp('des')
      return { services: s, notifications: { service: notificationsHttp('des'), apercu: lecteurApercu(s) } }
    }
    const demo = sessionDemo()
    const s = servicesSimules({ utilisateur: { id: demo.sujet, nom: demo.nom } })
    return { services: s, notifications: { service: notificationsDemo(s, { qc: client, moi: demo.sujet }), apercu: lecteurApercu(s) } }
  }, [reels, client])
  // « Mon compte » (D-62) : /bff/des/iam/moi, ou service simulé au nom du profil de démonstration
  // (gm.premierAccesDemo = 1 : démonstration du premier accès).
  const compte = useMemo(() => {
    if (reels) return monCompteHttp('des')
    const role = roleDemo()
    const p = PROFILS_DEMO[role]
    const demo = lireSessionDemo()
    const identifiant = demo?.identifiant || p.identifiant
    const [prenom, ...nom] = p.nom.split(' ')
    // Rattachement de démonstration (D-76) : unité et responsable de l'organisation simulée.
    const org = ORGANISATION_DEMO.find((m) => m.id === p.sujet)
    return monCompteSimule({
      couche: 'des',
      premierAcces: demo?.premierAcces ?? lirePreference('gm.premierAccesDemo') === '1',
      etapes: demo?.etapes,
      compte: { id: p.sujet, identifiant, prenom, nom: nom.join(' '), nomAffiche: p.nom, email: `${identifiant}@gimac-afr.org`, profils: [role], unite: org?.unite ?? null, responsableId: org?.responsableId ?? null },
      rattachement: rattachementDemo(p.sujet, p.nom),
    })
  }, [reels])
  // Administration des comptes (D-69) : /bff/des/iam, ou MS-IAM simulé au nom du profil de démonstration.
  const administration = useMemo(() => {
    if (reels) return servicesConsoleHttp('des')
    const s = sessionDemo()
    return servicesConsoleSimules({ acteur: { id: s.sujet, nom: s.nom, permissions: s.permissions } })
  }, [reels])
  const connexion = useMemo(() => (reels ? connexionHttp() : connexionDemo()), [reels])
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
        <ServicesConsoleProvider services={administration} chemins={CHEMINS_ADMINISTRATION}>
        <ExtensionsTableauDeBordProvider valeur={EXTENSIONS_TABLEAU}>
          <Routes>
            <Route path="/connexion/*" element={<PageConnexion couche="des" />} />
            <Route path="/des/*" element={<AvecSession>{(s) => <Pages session={s} etatCanal={etatCanal} correlationId={correlationId} notifications={notifications} />}</AvecSession>} />
            <Route path="*" element={<Navigate to="/des/tableau-de-bord" replace />} />
          </Routes>
        </ExtensionsTableauDeBordProvider>
        </ServicesConsoleProvider>
        </CompteProvider>
        </ConnexionProvider>
      )}
    </Fournisseurs>
  )
}
