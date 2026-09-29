import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { Card, EmptyState, Link, Skeleton, StatCard, StatusBadge, Tag } from '@gimac/design-system'
import { aPermission, usePermissions, useSession, type GroupeNavigation } from '@gimac/socle'
import { useIndicateursDdrm, useInstitutions } from '../../hooks/participant'
import { etatInstitution } from '../../dictionnaires'
import { NAVIGATION_DES } from '../../navigation'

// DES-01 Tableau de bord (/des/tableau-de-bord, gabarit G1 ; D-58, D-61) : chaque acteur voit son
// tableau de bord, composé des seules sections que la matrice des rôles de son jeton ouvre. Les
// compteurs viennent des services ; l'interface n'en calcule aucun.

const grille = { display: 'flex', flexWrap: 'wrap' as const, gap: 'var(--gm-space-4)' }

function Section({ titre, children }: { titre: string; children: ReactNode }) {
  return (
    <section className="gm-col" aria-label={titre}>
      <h2 className="gm-type-h3" style={{ margin: 0 }}>{titre}</h2>
      {children}
    </section>
  )
}

/** Chargé et chef de la DDRM : étapes 1 et 2 du parcours (prise de contact, adhésion). */
function SectionDdrm({ approbateur, instructeur }: { approbateur: boolean; instructeur: boolean }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const indicateurs = useIndicateursDdrm()
  const mesDossiers = useInstitutions({ page: 1, taille: 5, mesDossiers: true, statut: ['CANDIDAT'] })
  const i = indicateurs.data
  const vers = (statut: string) => () => void navigate(`/des/institutions?statut=${statut}`)
  return (
    <Section titre={t('tableau.ddrm.titre')}>
      {indicateurs.isPending ? (
        <Skeleton height={96} />
      ) : indicateurs.isError || !i ? (
        <StatusBadge status="crit" label={t('commun.erreur.titre')} />
      ) : (
        <div style={grille}>
          {approbateur ? (
            <StatCard
              label={t('tableau.ddrm.propositions')}
              value={i.propositionsEnAttente}
              status={i.propositionsEnAttente > 0 ? 'warn' : 'ok'}
              statusLabel={t(i.propositionsEnAttente > 0 ? 'tableau.aTrancher' : 'tableau.aJour')}
              onClick={vers('CANDIDAT')}
            />
          ) : null}
          <StatCard label={t('tableau.ddrm.candidats')} value={i.candidats} onClick={vers('CANDIDAT')} />
          {instructeur ? (
            <StatCard
              label={t('tableau.ddrm.piecesAExaminer')}
              value={i.piecesAExaminer}
              status={i.piecesAExaminer > 0 ? 'warn' : 'ok'}
              statusLabel={t(i.piecesAExaminer > 0 ? 'tableau.aTraiter' : 'tableau.aJour')}
              onClick={vers('CANDIDAT')}
            />
          ) : null}
          <StatCard label={t('tableau.ddrm.adherents')} value={i.adherents} onClick={vers('ADHERENT')} />
          <StatCard label={t('tableau.ddrm.enHomologation')} value={i.enHomologation} onClick={vers('EN_HOMOLOGATION')} />
          <StatCard label={t('tableau.ddrm.closSansSuite')} value={i.closSansSuite} onClick={vers('CLOS_SANS_SUITE')} />
        </div>
      )}
      {instructeur ? (
        <Card title={t('tableau.ddrm.mesDossiers')} actions={<Link href="/des/institutions?onglet=mesDossiers">{t('tableau.voirTout')}</Link>}>
          {mesDossiers.isPending ? (
            <Skeleton height={48} />
          ) : (mesDossiers.data?.contenu ?? []).length === 0 ? (
            <span className="gm-field-help">{t('tableau.ddrm.aucunDossier')}</span>
          ) : (
            <ul className="gm-col" style={{ listStyle: 'none', margin: 0, padding: 0 }}>
              {(mesDossiers.data?.contenu ?? []).map((d) => (
                <li key={d.id} className="gm-row" style={{ justifyContent: 'space-between' }}>
                  <Link href={`/des/institutions/${d.id}`}>{d.nom}</Link>
                  <StatusBadge status={etatInstitution[d.statut]?.status ?? 'neutral'} label={t(`institution.statut.${d.statut}`)} />
                </li>
              ))}
            </ul>
          )}
        </Card>
      ) : null}
    </Section>
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
    <Section titre={t('tableau.acces.titre')}>
      <div style={grille}>
        {entrees.map((e) => (
          <Card key={e.cle}>
            <Link href={e.chemin}>{t(e.libelleKey)}</Link>
          </Card>
        ))}
      </div>
    </Section>
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
  const rien = !ddrm && !pilotage && !lecteurRegistre
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-6)' }}>
      <header className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
        <h1 className="gm-heading-display">{t('tableau.titre')}</h1>
        {session ? (
          <div className="gm-row">
            <span>{t('tableau.bienvenue', { nom: session.nom })}</span>
            {profils.map((r) => (
              <Tag key={r}>{t(`profil.${r}`, { defaultValue: r })}</Tag>
            ))}
          </div>
        ) : null}
      </header>
      {ddrm ? <SectionDdrm approbateur={approbateur} instructeur={instructeur} /> : null}
      {pilotage ? (
        <Section titre={t('tableau.homologation.titre')}>
          <Card>
            <Link href="/des/liaisons?onglet=pilotage">{t('tableau.homologation.pilotage')}</Link>
          </Card>
        </Section>
      ) : null}
      <MesAcces permissions={permissions} />
      {rien ? <EmptyState icon="gauge" title={t('tableau.vide.titre')} text={t('tableau.vide.texte')} /> : null}
    </div>
  )
}
