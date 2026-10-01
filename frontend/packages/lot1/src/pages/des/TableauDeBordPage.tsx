import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Card, EmptyState, Link, LinkList, PageHeader, PageSection, Skeleton, StatGrid, StatusBadge, type StatCardProps } from '@gimac/design-system'
import { aPermission, usePermissions, useSession, type GroupeNavigation } from '@gimac/socle'
import { useIndicateursDdrm, useInstitutions } from '../../hooks/participant'
import { etatInstitution } from '../../dictionnaires'
import { NAVIGATION_DES } from '../../navigation'
import { fonctionsDe } from '../../permissions'
import { SectionActeur } from './tableau/SectionActeur'
import { SectionCockpit } from './tableau/SectionCockpit'
import { SectionAdministration } from './tableau/SectionAdministration'

// DES-01 Tableau de bord (/des/tableau-de-bord, gabarit G1 ; D-58, D-61) : chaque acteur voit son
// tableau de bord, composé des seules sections que la matrice des rôles de son jeton ouvre. Les
// compteurs viennent des services ; l'interface n'en calcule aucun. Composition du catalogue (D-63) :
// PageHeader, PageSection, StatGrid, LinkList.

type Tuile = StatCardProps & { key: string }

/** Chargé et chef de la DDRM : étapes 1 et 2 du parcours (prise de contact, adhésion). */
function SectionDdrm({ approbateur, instructeur }: { approbateur: boolean; instructeur: boolean }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const indicateurs = useIndicateursDdrm()
  // « Mes dossiers en cours » paginés côté serveur, pagination visible comme celle des tableaux.
  const [pageDossiers, setPageDossiers] = useState({ page: 1, taille: 5 })
  const mesDossiers = useInstitutions({ ...pageDossiers, mesDossiers: true, statut: ['CANDIDAT'] }, instructeur)
  const i = indicateurs.data
  const vers = (statut: string) => () => void navigate(`/des/institutions?statut=${statut}`)
  const tuiles: Tuile[] = i
    ? [
        ...(approbateur
          ? [{ key: 'propositions', label: t('tableau.ddrm.propositions'), value: i.propositionsEnAttente, status: i.propositionsEnAttente > 0 ? 'warn' as const : 'ok' as const,
              statusLabel: t(i.propositionsEnAttente > 0 ? 'tableau.aTrancher' : 'tableau.aJour'), onClick: vers('CANDIDAT') }]
          : []),
        { key: 'candidats', label: t('tableau.ddrm.candidats'), value: i.candidats, onClick: vers('CANDIDAT') },
        ...(instructeur
          ? [{ key: 'pieces', label: t('tableau.ddrm.piecesAExaminer'), value: i.piecesAExaminer, status: i.piecesAExaminer > 0 ? 'warn' as const : 'ok' as const,
              statusLabel: t(i.piecesAExaminer > 0 ? 'tableau.aTraiter' : 'tableau.aJour'), onClick: vers('CANDIDAT') }]
          : []),
        { key: 'adherents', label: t('tableau.ddrm.adherents'), value: i.adherents, onClick: vers('ADHERENT') },
        { key: 'enHomologation', label: t('tableau.ddrm.enHomologation'), value: i.enHomologation, onClick: vers('EN_HOMOLOGATION') },
        { key: 'closSansSuite', label: t('tableau.ddrm.closSansSuite'), value: i.closSansSuite, onClick: vers('CLOS_SANS_SUITE') },
      ]
    : []
  return (
    <PageSection title={t('tableau.ddrm.titre')}>
      {indicateurs.isPending ? (
        <Skeleton height={96} />
      ) : indicateurs.isError || !i ? (
        <StatusBadge status="crit" label={t('commun.erreur.titre')} />
      ) : (
        <StatGrid label={t('tableau.ddrm.titre')} items={tuiles} />
      )}
      {instructeur ? (
        <Card title={t('tableau.ddrm.mesDossiers')} actions={<Link href="/des/institutions?onglet=mesDossiers">{t('tableau.voirTout')}</Link>}>
          {mesDossiers.isPending ? (
            <Skeleton height={48} />
          ) : (
            <LinkList
              label={t('tableau.ddrm.mesDossiers')}
              empty={t('tableau.ddrm.aucunDossier')}
              page={pageDossiers.page}
              pageSize={pageDossiers.taille}
              total={mesDossiers.data?.total ?? 0}
              onPage={(page) => setPageDossiers((p) => ({ ...p, page }))}
              onPageSize={(taille) => setPageDossiers({ page: 1, taille })}
              items={(mesDossiers.data?.contenu ?? []).map((d) => ({
                key: d.id, href: `/des/institutions/${d.id}`, label: d.nom, status: etatInstitution[d.statut]?.status ?? 'neutral', statusLabel: t(`institution.statut.${d.statut}`),
              }))}
            />
          )}
        </Card>
      ) : null}
    </PageSection>
  )
}

/** Raccourcis vers les écrans que la matrice des rôles ouvre (navigation filtrée par permission). */
function MesAcces({ permissions }: { permissions: string[] | undefined }) {
  const { t } = useTranslation()
  const entrees = (NAVIGATION_DES as GroupeNavigation[])
    .flatMap((g) => g.entrees)
    .filter((e) => e.cle !== 'tableauDeBord' && aPermission(permissions, ...(e.permission ? [e.permission] : [])))
  if (entrees.length === 0) return null
  return (
    <PageSection title={t('tableau.acces.titre')}>
      <LinkList label={t('tableau.acces.titre')} items={entrees.map((e) => ({ key: e.cle, href: e.chemin, label: t(e.libelleKey) }))} />
    </PageSection>
  )
}

export default function TableauDeBordPage() {
  const { t } = useTranslation()
  const session = useSession()
  const permissions = usePermissions()
  const lecteurRegistre = aPermission(permissions, 'PAR_INSTITUTION_READ')
  const instructeur = lecteurRegistre && aPermission(permissions, 'PAR_ADHESION_INSTRUCT')
  const approbateur = lecteurRegistre && aPermission(permissions, 'PAR_TRANSITION_APPROVE')
  const ddrm = instructeur || approbateur
  const pilotage = aPermission(permissions, 'HOM_DASHBOARD_READ')
  const profils = (session?.roles ?? []).filter((r) => r !== 'ROLE_USER')
  const fonctions = fonctionsDe(session?.roles ?? [])
  // « Mon travail » (étapes 3 à 10) couvre le cadrage, « Mes projets » et les gestes de chaque fonction.
  const travail = fonctions.length > 0 && aPermission(permissions, 'HOM_LIAISON_LIST')
  const cockpit = pilotage && fonctions.some((f) => f === 'DG' || f === 'CHEF_DES' || f === 'CHEF_DDRM')
  // Administration des comptes (D-62) : liens vers la console pour les porteurs de IAM_ACCOUNT_READ (RSSI).
  const administration = aPermission(permissions, 'IAM_ACCOUNT_READ')
  const rien = !ddrm && !travail && !cockpit && !lecteurRegistre && !administration
  return (
    <div className="gm-col">
      <PageHeader
        title={t('tableau.titre')}
        subtitle={t('tableau.sousTitre')}
        user={session ? { name: session.nom, roles: profils.map((r) => t(`profil.${r}`, { defaultValue: r })) } : undefined}
      />
      {administration ? <SectionAdministration /> : null}
      {ddrm ? <SectionDdrm approbateur={approbateur} instructeur={instructeur} /> : null}
      {cockpit ? <SectionCockpit /> : null}
      {travail ? <SectionActeur fonctions={fonctions} /> : null}
      {/* Raccourcis seulement pour un profil sans section de travail (lecture seule) : la navigation les porte déjà. */}
      {!travail ? <MesAcces permissions={permissions} /> : null}
      {rien ? <EmptyState icon="gauge" title={t('tableau.vide.titre')} text={t('tableau.vide.texte')} /> : null}
    </div>
  )
}
