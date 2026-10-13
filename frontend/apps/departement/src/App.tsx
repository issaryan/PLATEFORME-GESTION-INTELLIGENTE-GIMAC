import { lazy, Suspense, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, Route, Routes, useLocation, useParams } from 'react-router'
import { Skeleton } from '@gimac/design-system'
import {
  Aiguillage, ConnexionProvider, Enveloppe, aPermission, GardeSession, NotificationsPage, NotificationsProvider, PageConnexion, PageDeconnexion, SessionActive, adresseConnexion, configureHttp,
  connexionHttp, lirePreference, lireSessionDemo, notificationsHttp, surNonAutorise, type EtatCanal, type LecteurApercu, type ServiceNotifications, type Session,
} from '@gimac/socle'
import { CompteProvider, GardePremierAcces, MonComptePage, useMonAvatar, monCompteHttp, monCompteSimule } from '@gimac/compte'
import {
  CatalogueFonctionnalitesProvider, ECRANS_DES, ExtensionsTableauDeBordProvider, Fournisseurs, GardeFonctionnalite, GardePermission, ORGANISATION_DEMO, PERM, creerClientRequetes, lecteurApercu,
  magasinPouvoirs, navigationDepuis, navigationRepli, notificationsDemo, rattachementDemo, roleDeCleDemo, servicesHttp, servicesSimules, useNonLus, useSourcesRecherche, useTraduire,
  type EtatCatalogue,
} from '@gimac/lot1'
import {
  ServicesConsoleProvider, TableauAdministrationComptes, WidgetChargesEnErreur, WidgetSupervision, servicesConsoleHttp, servicesConsoleSimules, useFonctionnalites, useSourceComptes,
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
const ClotureOperationPage = lazy(() => import('@gimac/lot1/pages/des/ClotureOperationPage'))
const DossierMiseEnServicePage = lazy(() => import('@gimac/lot1/pages/des/DossierMiseEnServicePage'))
/** Ancienne adresse d'une liaison : renvoi vers son dossier de parcours. */
function VersDossier() {
  const { id = '' } = useParams()
  return <Navigate to={`/des/parcours/${id}`} replace />
}
const ParcoursPage = lazy(() => import('@gimac/lot1/pages/des/parcours/ParcoursPage'))
const DossierLiaisonPage = lazy(() => import('@gimac/lot1/pages/des/parcours/DossierLiaisonPage'))
// Parcours de certification (D-98, docs/lot1/lancement-processus.md §8) : lancement guidé et « Mes actions de certification ».
const LancerCertificationPage = lazy(() => import('@gimac/lot1/pages/des/certification/LancerCertificationPage'))
const MesActionsCertificationPage = lazy(() => import('@gimac/lot1/pages/des/certification/MesActionsCertificationPage'))
const ReferentielsOperationsPage = lazy(() => import('@gimac/lot1/pages/des/ReferentielsOperationsPage'))
// Types de projet et catalogue des fonctions (D-91).
const TypesProjetPage = lazy(() => import('@gimac/lot1/pages/des/referentiels/TypesProjetPage'))
const MessageriePage = lazy(() => import('@gimac/lot1/pages/des/messagerie/MessageriePage'))
// Mon activité (D-76, docs/lot1/mon-activite.md) : journée, agenda, tâches, rapports et équipe de chacun.
const MaJourneePage = lazy(() => import('@gimac/lot1/pages/des/activite/MaJourneePage'))
const AgendaPage = lazy(() => import('@gimac/lot1/pages/des/activite/AgendaPage'))
const TachesPage = lazy(() => import('@gimac/lot1/pages/des/activite/TachesPage'))
const RapportsPage = lazy(() => import('@gimac/lot1/pages/des/activite/RapportsPage'))
const EquipePage = lazy(() => import('@gimac/lot1/pages/des/activite/EquipePage'))
// Processus d'unité (D-87, docs/lot1/processus-unite.md) : catalogue, modélisateur, suivi d'une instance.
const ProcessusPage = lazy(() => import('@gimac/lot1/pages/des/processus/ProcessusPage'))
const ModelisateurProcessusPage = lazy(() => import('@gimac/lot1/pages/des/processus/ModelisateurProcessusPage'))
const InstanceProcessusPage = lazy(() => import('@gimac/lot1/pages/des/processus/InstancePage'))
// Lancer une demande et suivre ses demandes (D-98, docs/lot1/lancement-processus.md §3, §7).
const LancerDemandePage = lazy(() => import('@gimac/lot1/pages/des/processus/LancerDemandePage'))
const MesDemandesPage = lazy(() => import('@gimac/lot1/pages/des/processus/MesDemandesPage'))
// Administration des comptes (D-69) : écrans de @gimac/administration servis sous /des/administration,
// pour le RSSI et le super administrateur ; la console garde la supervision et la configuration.
const ComptesPage = lazy(() => import('@gimac/administration/pages/ComptesPage'))
const ComptePage = lazy(() => import('@gimac/administration/pages/ComptePage'))
const CreationComptePage = lazy(() => import('@gimac/administration/pages/CreationComptePage'))
const JournalAdministrationPage = lazy(() => import('@gimac/administration/pages/JournalPage'))
const ProfilsPage = lazy(() => import('@gimac/administration/pages/ProfilsPage'))
const PolitiqueSecuritePage = lazy(() => import('@gimac/administration/pages/PolitiqueSecuritePage'))
const OrganisationPage = lazy(() => import('@gimac/administration/pages/OrganisationPage'))
// Organigramme (D-86 §3) : unités, chefs, membres et n+1, pouvoirs effectifs.
const OrganigrammePage = lazy(() => import('@gimac/administration/pages/OrganigrammePage'))
// Pouvoirs hiérarchiques (D-81) : demandes des chefs à traiter, matrice des pouvoirs.
const DemandesPage = lazy(() => import('@gimac/administration/pages/DemandesPage'))
const MatricePouvoirsPage = lazy(() => import('@gimac/administration/pages/MatricePouvoirsPage'))
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

/**
 * Écran d'une fonctionnalité du catalogue servi par MS-IAM (D-94) : ouvert si la configuration le rend visible et que
 * les permissions satisfont son prédicat (et détiennent `droit`) ; refus explicite, la passerelle refait le contrôle.
 */
const G = (cle: string, page: ReactNode, droit?: string) => <GardeFonctionnalite cle={cle} droit={droit}>{page}</GardeFonctionnalite>

/** État du catalogue des fonctionnalités : lu sur /bff/des/iam/configuration/fonctionnalites ; échec : repli prudent. */
function useEtatCatalogue(actif: boolean): EtatCatalogue {
  const q = useFonctionnalites(actif)
  // Réponse qui n'est pas une liste : tenue pour indisponible (repli prudent), comme un échec.
  return useMemo(() => (Array.isArray(q.data) ? { etat: 'pret', fonctionnalites: q.data } : q.isError || q.data ? { etat: 'indisponible', fonctionnalites: [] } : { etat: 'chargement', fonctionnalites: [] }), [q.data, q.isError])
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
  // Barre latérale dérivée du catalogue servi (D-94) ; l'accueil seul tant qu'il n'est pas servi.
  const catalogue = useEtatCatalogue(!session.restreinte)
  const tr = useTraduire()
  const navigation = useMemo(() => (catalogue.etat === 'pret' ? navigationDepuis(catalogue.fonctionnalites, 'des', ECRANS_DES, tr, 'nav.groupe.') : navigationRepli('des')), [catalogue, tr])
  return (
    <CatalogueFonctionnalitesProvider valeur={catalogue}>
    <GardePremierAcces>
      {(assistant) => (
        // Cloche, fenêtres surgissantes et page des notifications (D-80) : NOT_INBOX_READ, hors premier accès.
        <NotificationsProvider service={notifications.service} couche="des" actif={!assistant && aPermission(p, PERM.notificationsLire)} apercu={notifications.apercu}>
        <Enveloppe
          avatar={moi?.src}
          fonction={moi?.fonction}
          couche="des"
          navigation={assistant ? [] : navigation}
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
                  <Route path="institutions" element={G('institutions', <InstitutionsPage />)} />
                  <Route path="institutions/:id" element={G('institutions', <InstitutionPage />)} />
                  <Route path="liaisons" element={G('liaisons', <LiaisonsPage />)} />
                  {/* Fiche d'une liaison : le dossier du parcours (routes réelles du service), l'ancienne fiche à onglets n'ayant pas de service. */}
                  <Route path="liaisons/:id" element={G('liaisons', <VersDossier />)} />
                  <Route path="liaisons/:id/operations/:op/cloture" element={G('liaisons', <ClotureOperationPage />)} />
                  <Route path="liaisons/:id/mise-en-service" element={G('liaisons', <DossierMiseEnServicePage />)} />
                  <Route path="parcours" element={G('liaisons', <ParcoursPage />)} />
                  <Route path="parcours/:id" element={G('liaisons', <DossierLiaisonPage />)} />
                  <Route path="certification/lancer" element={G('lancerCertification', <LancerCertificationPage />)} />
                  <Route path="certification/mes-actions" element={G('mesActionsCertification', <MesActionsCertificationPage />)} />
                  <Route path="messagerie" element={G('messagerie', <MessageriePage />)} />
                  <Route path="notifications" element={G('notifications', <NotificationsPage />)} />
                  <Route path="activite" element={G('maJournee', <MaJourneePage />)} />
                  <Route path="agenda" element={G('agenda', <AgendaPage />)} />
                  <Route path="taches" element={G('taches', <TachesPage />)} />
                  <Route path="rapports" element={G('rapports', <RapportsPage />)} />
                  <Route path="equipe" element={G('equipe', <EquipePage />)} />
                  <Route path="equipe/demandes" element={G('equipe', <EquipePage />)} />
                  <Route path="processus" element={G('processus', <ProcessusPage />)} />
                  {/* D-98 : lien des notifications et des tâches, ouvert à tout compte interne (PIL_ACTIVITY_OWN), même si « Processus de l'unité » est masquée ; MS-PILOTAGE contrôle la lecture. */}
                  <Route path="processus/instances/:id" element={<GardePermission permission={PERM.activitePropre}><InstanceProcessusPage /></GardePermission>} />
                  <Route path="processus/:id" element={G('processus', <ModelisateurProcessusPage />)} />
                  <Route path="demandes" element={G('mesDemandes', <MesDemandesPage />)} />
                  <Route path="demandes/lancer" element={G('lancerDemande', <LancerDemandePage />)} />
                  <Route path="demandes/:id" element={<GardePermission permission={PERM.activitePropre}><InstanceProcessusPage /></GardePermission>} />
                  <Route path="referentiels/operations" element={G('referentiels', <ReferentielsOperationsPage />)} />
                  <Route path="referentiels/types-projet" element={G('referentiels', <TypesProjetPage />)} />
                  <Route path="administration/comptes" element={G('adminComptes', <ComptesPage />)} />
                  <Route path="administration/comptes/nouveau" element={G('adminComptes', <CreationComptePage />, 'IAM_ACCOUNT_CREATE')} />
                  <Route path="administration/comptes/:id" element={G('adminComptes', <ComptePage />)} />
                  <Route path="administration/journal" element={G('adminJournal', <JournalAdministrationPage />)} />
                  <Route path="administration/profils" element={G('adminProfils', <ProfilsPage />)} />
                  <Route path="administration/demandes" element={G('adminDemandes', <DemandesPage />)} />
                  <Route path="administration/matrice-pouvoirs" element={G('adminMatrice', <MatricePouvoirsPage />)} />
                  <Route path="administration/organisation" element={G('adminOrganisation', <OrganisationPage />)} />
                  <Route path="administration/organigramme" element={G('adminOrganigramme', <OrganigrammePage />)} />
                  <Route path="administration/politique-securite" element={G('adminPolitique', <PolitiqueSecuritePage />)} />
                  <Route path="plateforme/supervision" element={G('supervision', <SupervisionPage />)} />
                  <Route path="plateforme/infrastructure" element={G('infrastructure', <InfrastructurePage />)} />
                  <Route path="plateforme/configuration" element={G('configuration', <ConfigurationPlateformePage />)} />
                  <Route path="plateforme/journalisation" element={G('journalisation', <JournalisationPage />)} />
                  <Route path="plateforme/courriels" element={G('courriels', <CourrielsPage />)} />
                  <Route path="*" element={<Navigate to="/des/tableau-de-bord" replace />} />
                </>
              )}
            </Routes>
          </Suspense>
        </Enveloppe>
        </NotificationsProvider>
      )}
    </GardePremierAcces>
    </CatalogueFonctionnalitesProvider>
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
  // Pouvoirs hiérarchiques simulés (D-81) : un magasin pour la page, commun à « Mon équipe » et à l'administration.
  const pouvoirs = useMemo(() => (reels ? undefined : magasinPouvoirs()), [reels])
  // Notifications de la plateforme (D-80) : /bff/des/notification, ou MS-NOTIFICATION simulé branché sur la
  // messagerie simulée (un message reçu crée ou regroupe une notification).
  const { services, notifications } = useMemo(() => {
    if (reels) {
      const s = servicesHttp('des')
      return { services: s, notifications: { service: notificationsHttp('des'), apercu: lecteurApercu(s) } }
    }
    const demo = sessionDemo()
    const s = servicesSimules({ utilisateur: { id: demo.sujet, nom: demo.nom, profils: demo.roles }, pouvoirs })
    return { services: s, notifications: { service: notificationsDemo(s, { qc: client, moi: demo.sujet, profils: demo.roles }), apercu: lecteurApercu(s) } }
  }, [reels, client, pouvoirs])
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
      compte: { id: p.sujet, identifiant, prenom, nom: nom.join(' '), nomAffiche: p.nom, email: `${identifiant}@gimac-afr.org`, profils: [roleDeCleDemo(role)], unite: org?.unite ?? null, responsableId: org?.responsableId ?? null },
      rattachement: rattachementDemo(p.sujet, p.nom),
    })
  }, [reels])
  // Administration des comptes (D-69) : /bff/des/iam, ou MS-IAM simulé au nom du profil de démonstration.
  const administration = useMemo(() => {
    if (reels) return servicesConsoleHttp('des')
    const s = sessionDemo()
    return servicesConsoleSimules({ acteur: { id: s.sujet, nom: s.nom, permissions: s.permissions }, pouvoirs })
  }, [reels, pouvoirs])
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
