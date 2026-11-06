import { useTranslation } from 'react-i18next'
import { Card, CoverageBar, DataTable, InlineAlert, ProgressRing, StatusBadge, Tag, type Column } from '@gimac/design-system'
import type { CoverageValue } from '../../../../modele/commun'
import type { DossierLiaison, EtatServicePerimetre, ServicePerimetre } from '../../../../modele/parcours'
import { Jour, tonService } from '../communs'
import { GestionPerimetre } from '../../liaisons/perimetre'
import { decrire } from '../../../../dictionnaires/services-perimetre'

// Onglet « Périmètre », écran principal du dossier (CR §7.2.2) : l'état du périmètre contractualisé,
// service par service, avec les deux couvertures calculées par le service (certification, mise en
// service). Pour la branche mobile, l'avancement est repris du simulateur, jamais recalculé (CR §4.4.1).

/** Niveau de la barre de couverture du catalogue pour un état servi (traduction d'état, sans calcul). */
const NIVEAU: Record<EtatServicePerimetre, 1 | 2 | 3 | 4> = { NON_DEMARRE: 1, EN_TEST: 2, CERTIFIE: 3, NON_OUVERT: 3, OUVERT: 4 }

function Anneau({ libelle, valeur, detail }: { libelle: string; valeur: CoverageValue; detail: string }) {
  const { t } = useTranslation()
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-2)', alignItems: 'flex-start', flex: '1 1 220px' }}>
      <ProgressRing label={libelle} value={valeur.calculable ? valeur.taux : null} size={112} status={valeur.taux === 100 ? 'ok' : undefined} statusLabel={t('parcoursPerimetre.complete')} />
      <span className="gm-field-help">{valeur.calculable ? detail : t('parcoursPerimetre.nonCalculable', { motif: valeur.motifNonCalculable ?? '' })}</span>
    </div>
  )
}

export function OngletPerimetre({ dossier }: { dossier: DossierLiaison }) {
  const { t, i18n } = useTranslation()
  const nom = (s: ServicePerimetre) => (decrire(s.serviceCode, dossier.typeProjet, i18n.language).connu ? decrire(s.serviceCode, dossier.typeProjet, i18n.language).libelle : s.libelle)
  const mobile = dossier.perimetre.some((s) => s.avancementSimulateur !== null)
  const inclus = dossier.perimetre.filter((s) => s.inclus)
  const c = dossier.couvertureCertification
  const m = dossier.couvertureMiseEnService
  const colonnes: Column<ServicePerimetre>[] = [
    {
      key: 'libelle', header: t('parcoursPerimetre.service'),
      render: (s) => (
        // D-133 : nom usuel en clair, code technique en infobulle seulement.
        <span title={t('perimetreProjet.code', { code: s.serviceCode })}>{nom(s)}</span>
      ),
    },
    { key: 'inclus', header: t('parcoursPerimetre.inclusion'), render: (s) => <StatusBadge status={s.inclus ? 'info' : 'neutral'} icon={null} label={t(s.inclus ? 'parcoursPerimetre.inclus' : 'parcoursPerimetre.exclu')} /> },
    { key: 'etat', header: t('parcoursPerimetre.etat'), render: (s) => (s.inclus ? <StatusBadge status={tonService[s.etat]} label={t(`parcours.servicePerimetre.${s.etat}`)} /> : <span className="gm-field-help">{t('parcoursPerimetre.sansObjet')}</span>) },
    { key: 'entreeEssai', header: t('parcoursPerimetre.entreeEssai'), render: (s) => <Jour iso={s.entreeEssai} absent={t(s.inclus ? 'parcoursPerimetre.pasEncore' : 'parcoursPerimetre.sansObjet')} /> },
    { key: 'sortieEssai', header: t('parcoursPerimetre.sortieEssai'), render: (s) => <Jour iso={s.sortieEssai} absent={t(s.inclus ? 'parcoursPerimetre.pasEncore' : 'parcoursPerimetre.sansObjet')} /> },
    { key: 'certifieLe', header: t('parcoursPerimetre.certifieLe'), render: (s) => <Jour iso={s.certifieLe} absent={t(s.inclus ? 'parcoursPerimetre.pasEncore' : 'parcoursPerimetre.sansObjet')} /> },
    ...(mobile
      ? [{
          key: 'avancementSimulateur', header: t('parcoursPerimetre.simulateur'),
          render: (s: ServicePerimetre) =>
            s.avancementSimulateur === null ? (
              <span className="gm-field-help">{t('parcoursPerimetre.pasEncore')}</span>
            ) : (
              <span className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'nowrap' }}>
                <ProgressRing compact label={t('parcoursPerimetre.simulateur')} value={s.avancementSimulateur} />
                <Tag>{t('parcoursPerimetre.reprisSimulateur')}</Tag>
              </span>
            ),
        }]
      : []),
  ]
  return (
    <div className="gm-col">
      <GestionPerimetre liaison={{ id: dossier.id, typeProjet: dossier.typeProjet, institutionNom: dossier.institutionNom, intituleProjet: dossier.intituleProjet }} />
      <Card title={t('parcoursPerimetre.couvertures')}>
        <div className="gm-row" style={{ gap: 'var(--gm-space-6)', alignItems: 'flex-start' }}>
          {dossier.avancement ? <Anneau libelle={t('parcoursListe.avancement')} valeur={dossier.avancement} detail={t('parcoursPerimetre.detailAvancement')} /> : null}
          <Anneau libelle={t('parcoursListe.couvertureCertification')} valeur={c} detail={t('parcoursPerimetre.detailCertification', { n: c.numerateur, d: c.denominateur })} />
          <Anneau libelle={t('parcoursListe.couvertureMiseEnService')} valeur={m} detail={t('parcoursPerimetre.detailMiseEnService', { n: m.numerateur, d: m.denominateur })} />
          <div className="gm-col" style={{ gap: 'var(--gm-space-2)', flex: '2 1 280px', minWidth: 0 }}>
            {inclus.map((s) => (
              <CoverageBar key={s.serviceCode} service={nom(s)} level={NIVEAU[s.etat]} />
            ))}
          </div>
        </div>
      </Card>
      {mobile ? <InlineAlert level="info">{t('parcours.alerte.simulateur')}</InlineAlert> : null}
      <Card title={t('parcoursPerimetre.titre', { n: inclus.length, total: dossier.perimetre.length })}>
        <DataTable
          columns={colonnes}
          rows={dossier.perimetre}
          rowKey={(s) => s.serviceCode}
          state={dossier.perimetre.length ? 'ok' : 'empty'}
          emptyText={t('parcoursPerimetre.vide')}
          emptyHelp={t('parcoursPerimetre.vide.aide')}
        />
      </Card>
    </div>
  )
}
