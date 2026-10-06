import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import { ActionBar, BarresHorizontales, Button, DataTable, Link, PageSection, SerieTemporelle, StatGrid, type Column } from '@gimac/design-system'
import { aPermission, useLibelleProfil, usePermissions } from '@gimac/socle'
import { dateHeure } from '@gimac/compte'
import { useIndicateursComptes, useJournal, useLibellesProfils } from '../hooks'
import type { EntreeJournal, IndicateursComptes, ParJour } from '../modele/comptes'
import { useChemins } from '../services/useServicesConsole'
import { Donnees, Section } from './communs'
import { DialogueCreationCompte } from './DialogueCreationCompte'

// Sections d'administration des tableaux de bord (D-62, D-69), communes à la console (/console) et au
// département (/des/administration) : gestion des utilisateurs (actions directes, indicateurs cliquables
// vers la liste filtrée) et derniers actes d'administration. Chemins de l'application courante (useChemins).

/**
 * Gestion des utilisateurs (D-62), en tête du tableau de bord du super administrateur et du RSSI :
 * actions directes (création si IAM_ACCOUNT_CREATE, listes filtrées, profils, journal, politique) et
 * indicateurs des comptes cliquables vers la liste filtrée (?etat=). La création s'ouvre en modale.
 */
export function SectionGestionUtilisateurs({ posture }: { posture: boolean }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { base } = useChemins()
  const permissions = usePermissions()
  const libelleProfil = useLibelleProfil(useLibellesProfils())
  const indicateurs = useIndicateursComptes()
  const [creation, setCreation] = useState(false)
  const peut = (p: string) => aPermission(permissions, p)
  const vers = (chemin: string) => () => void navigate(chemin)
  const alerte = (n: number, s: 'crit' | 'warn', libelle: string) => ({ status: n > 0 ? s : ('ok' as const), statusLabel: t(n > 0 ? libelle : 'console.aJour') })
  return (
    <PageSection title={t('console.gestion.titre')} description={t(posture ? 'console.gestion.texte.rssi' : 'console.gestion.texte')}>
      <ActionBar align="start" label={t('console.gestion.actions')}>
        {peut('IAM_ACCOUNT_CREATE') ? <Button variant="primary" icon="plus" onClick={() => setCreation(true)}>{t('console.comptes.creer')}</Button> : null}
        <Button onClick={vers(`${base}/comptes`)}>{t('console.gestion.tous')}</Button>
        <Button variant="ghost" onClick={vers(`${base}/comptes?etat=SANS_2FA`)}>{t('console.gestion.sans2fa')}</Button>
        <Button variant="ghost" onClick={vers(`${base}/comptes?etat=PREMIER_ACCES`)}>{t('console.gestion.premierAcces')}</Button>
        <Button variant="ghost" onClick={vers(`${base}/profils`)}>{t('console.nav.profils')}</Button>
        {peut('IAM_JOURNAL_READ') ? <Button variant="ghost" onClick={vers(`${base}/journal`)}>{t('console.nav.journal')}</Button> : null}
        {peut('PLATFORM_SECURITY_POLICY_READ') ? <Button variant="ghost" onClick={vers(`${base}/politique-securite`)}>{t('console.nav.politique')}</Button> : null}
      </ActionBar>
      <Donnees requete={indicateurs}>
        {(i) => (
          <div className="gm-col">
            <StatGrid
              label={t(posture ? 'console.posture.titre' : 'console.comptes.indicateurs')}
              items={[
                { key: 'total', label: t('console.indicateur.total'), value: i.total, onClick: vers(`${base}/comptes`) },
                { key: 'actifs', label: t('console.indicateur.actifs'), value: i.actifs, onClick: vers(`${base}/comptes?etat=ACTIF`) },
                { key: 'sans2fa', label: t('console.indicateur.sansSecondFacteur'), value: i.sansSecondFacteur, ...alerte(i.sansSecondFacteur, 'crit', 'console.aTraiter'), onClick: vers(`${base}/comptes?etat=SANS_2FA`) },
                { key: 'premierAcces', label: t('console.indicateur.premierAcces'), value: i.premierAccesEnAttente, ...alerte(i.premierAccesEnAttente, 'warn', 'console.enAttente'), onClick: vers(`${base}/comptes?etat=PREMIER_ACCES`) },
                { key: 'inactifs', label: t('console.indicateur.inactifs'), value: i.inactifs, onClick: vers(`${base}/comptes?etat=INACTIF`) },
                { key: 'privilegies', label: t('console.indicateur.privilegies'), value: i.privilegies, onClick: vers(`${base}/profils`) },
                ...(posture ? [{ key: 'crees30', label: t('console.indicateur.crees30'), value: i.creesDepuis30Jours, onClick: vers(`${base}/comptes`) }] : []),
                { key: 'actes24', label: t('console.indicateur.actes24'), value: i.actesDepuis24Heures, ...(peut('IAM_JOURNAL_READ') ? { onClick: vers(`${base}/journal`) } : {}) },
              ]}
            />
            <div className="gm-graphiques-grille">
              <BarresHorizontales
                title={t('console.indicateur.parProfil')}
                subtitle={t('console.indicateur.parProfil.aide')}
                maximum={Object.keys(i.parProfil).length}
                elements={Object.entries(i.parProfil).map(([p, n]) => ({ cle: p, libelle: libelleProfil(p), valeur: n, onClick: vers(`${base}/comptes?profil=${encodeURIComponent(p)}`) }))}
                vide={t('console.indicateur.parProfil.vide')}
              />
              <ActiviteComptes i={i} />
            </div>
          </div>
        )}
      </Donnees>
      {creation ? <DialogueCreationCompte onFermer={() => setCreation(false)} /> : null}
    </PageSection>
  )
}

/** Jour ISO (AAAA-MM-JJ) en minuit local : un point par jour sur l'axe du temps. */
const pointsJour = (l: ParJour[]) => l.map((x) => { const [a, m, j] = x.jour.split('-').map(Number); return { t: new Date(a, m - 1, j).getTime(), v: x.nombre } })

/** Activité des 30 derniers jours : comptes créés et actes d'administration par jour (MS-IAM). */
function ActiviteComptes({ i }: { i: IndicateursComptes }) {
  const { t } = useTranslation()
  if (!i.creationsParJour?.length && !i.actesParJour?.length) return null
  return (
    <SerieTemporelle
      title={t('console.activite.titre')}
      subtitle={t('console.activite.sousTitre')}
      granularite="jour"
      aire={false}
      min={0}
      format={(v) => (Number.isInteger(v) ? String(v) : '')}
      series={[
        { nom: t('console.activite.creations'), points: pointsJour(i.creationsParJour ?? []) },
        { nom: t('console.activite.actes'), points: pointsJour(i.actesParJour ?? []) },
      ]}
    />
  )
}

export function SectionActes() {
  const { t, i18n } = useTranslation()
  const { base } = useChemins()
  const journal = useJournal({ page: 1, taille: 5 })
  const colonnes: Column<EntreeJournal>[] = [
    { key: 'horodatage', header: t('console.journal.horodatage'), render: (j) => dateHeure(j.horodatage, i18n.language) },
    { key: 'acteurNom', header: t('console.journal.acteur'), render: (j) => j.acteurNom ?? j.acteurId },
    { key: 'action', header: t('console.journal.action'), render: (j) => t(`console.journal.action.${j.action}`, { defaultValue: j.action }) },
    { key: 'cibleLibelle', header: t('console.journal.cible'), render: (j) => <span className="gm-mono">{j.cibleLibelle ?? '—'}</span> },
    { key: 'motif', header: t('console.journal.motif'), render: (j) => j.motif ?? '—' },
  ]
  return (
    <Section titre={t('console.actes.titre')} id="gm-actes" action={<Link href={`${base}/journal`}>{t('console.actes.voirTout')}</Link>}>
      <Donnees requete={journal} estVide={(p) => p.contenu.length === 0} vide={t('console.journal.vide')}>
        {(p) => <DataTable columns={colonnes} rows={p.contenu} rowKey={(j) => j.id} density="compact" />}
      </Donnees>
    </Section>
  )
}

/**
 * Tableau de bord d'administration des comptes, tel que le département l'affiche (D-69) : gestion des
 * utilisateurs (posture de sécurité pour le RSSI) puis derniers actes, selon les permissions.
 */
export function TableauAdministrationComptes() {
  const permissions = usePermissions()
  const posture = !aPermission(permissions, 'IAM_ROLE_PRIVILEGED_GRANT')
  return (
    <>
      <SectionGestionUtilisateurs posture={posture} />
      {aPermission(permissions, 'IAM_JOURNAL_READ') ? <SectionActes /> : null}
    </>
  )
}
