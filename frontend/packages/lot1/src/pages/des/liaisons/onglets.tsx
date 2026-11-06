import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { DagView, Hash, InlineAlert, KeyValueList, LiaisonProgress, ResourceList } from '@gimac/design-system'
import type { Liaison, OperationInstance } from '../../../modele/homologation'
import {
  useCampagnesLiaison, useCouverturesLiaison, useDernierExportPreuves, useGrapheLiaison, useOperationsLiaison, useOptionsCollaborateurs, useScenariosInconnus,
} from '../../../hooks/liaisons'
import { GestionPerimetre } from './perimetre'
import { grapheVersDagView, servicesVersCoverage, taux } from '../../../adaptateurs/liaison'
import { useDescripteurDS } from '../../../ressources/descripteur'
import { creerOperationDescriptor } from '../../../ressources/operation'
import { campagneDescriptor, scenarioInconnuDescriptor } from '../../../ressources/campagne'
import { Donnees, etatListe } from './communs'
import { FormulaireAttribution } from './formulaires'

// Onglets de la vue d'une liaison (DES-09) : chacun obtient ses données par un hook et les rend avec
// les composants du catalogue, dans les quatre états.

export function OngletGraphe({ liaison }: { liaison: Liaison }) {
  const { t } = useTranslation()
  const graphe = useGrapheLiaison(liaison.id)
  return (
    <Donnees requete={graphe} estVide={(g) => g.noeuds.length === 0} vide={t('graphe.vide')}>
      {(g) => <DagView {...grapheVersDagView(g, t)} />}
    </Donnees>
  )
}

export function OngletPerimetre({ liaison }: { liaison: Liaison }) {
  const { t } = useTranslation()
  const couvertures = useCouverturesLiaison(liaison.id)
  const nonCalculables = [liaison.couvertureCertification, liaison.couvertureMiseEnService].filter((c) => !c.calculable)
  return (
    <div className="gm-col">
      <GestionPerimetre liaison={liaison} />
      {nonCalculables.map((c, i) => (
        <InlineAlert key={i} level="warn">{t('couverture.nonCalculable', { motif: c.motifNonCalculable ?? '' })}</InlineAlert>
      ))}
      <Donnees requete={couvertures} estVide={(s) => s.length === 0} vide={t('perimetre.vide')}>
        {(services) => (
          <LiaisonProgress
            certification={taux(liaison.couvertureCertification) ?? 0}
            commissioning={taux(liaison.couvertureMiseEnService) ?? 0}
            phase={liaison.phaseCourante}
            referential={String(liaison.versionNumero)}
            services={servicesVersCoverage(services)}
          />
        )}
      </Donnees>
    </div>
  )
}

export function OngletOperations({ liaison, avecPreuve = false }: { liaison: Liaison; avecPreuve?: boolean }) {
  const { t } = useTranslation()
  const collaborateurs = useOptionsCollaborateurs()
  const operations = useOperationsLiaison(liaison.id, avecPreuve ? { avecPreuve: true } : undefined)
  const [attribution, setAttribution] = useState<OperationInstance | null>(null)
  const lot = useMemo(() => creerOperationDescriptor({ t, collaborateurs: collaborateurs.data ?? [] }), [t, collaborateurs.data])
  const pilotes = useMemo(() => ({ openForm: (_m: 'create' | 'edit', r: OperationInstance) => setAttribution(r) }), [])
  const descripteur = useDescripteurDS(lot, pilotes)
  const lignes = operations.data ?? []
  return (
    <div className="gm-col">
      <ResourceList descriptor={descripteur} rows={lignes} embedded {...etatListe(operations, lignes)} />
      {attribution ? <FormulaireAttribution descripteur={descripteur} operation={attribution} onFermer={() => setAttribution(null)} /> : null}
    </div>
  )
}

export function OngletResultats({ liaison }: { liaison: Liaison }) {
  const { t } = useTranslation()
  const campagnes = useCampagnesLiaison(liaison.id)
  const scenarios = useScenariosInconnus(liaison.id)
  const dCampagnes = useDescripteurDS(campagneDescriptor)
  const dScenarios = useDescripteurDS(scenarioInconnuDescriptor)
  const lignesCampagnes = campagnes.data ?? []
  const lignesScenarios = scenarios.data ?? []
  return (
    <div className="gm-col">
      <InlineAlert level="info">{t('resultats.reprisNonRecalcules')}</InlineAlert>
      <h2 className="gm-card-title">{t('resultats.campagnes')}</h2>
      <ResourceList descriptor={dCampagnes} rows={lignesCampagnes} embedded {...etatListe(campagnes, lignesCampagnes)} />
      <h2 className="gm-card-title">{t('resultats.scenariosInconnus')}</h2>
      <ResourceList descriptor={dScenarios} rows={lignesScenarios} embedded {...etatListe(scenarios, lignesScenarios)} />
    </div>
  )
}

export function OngletPreuves({ liaison }: { liaison: Liaison }) {
  const { t } = useTranslation()
  const dernier = useDernierExportPreuves(liaison.id)
  return (
    <div className="gm-col">
      <h2 className="gm-card-title">{t('preuves.dernierExport')}</h2>
      <Donnees requete={dernier} estVide={(e) => e === null} vide={t('preuves.aucunExport')}>
        {(e) =>
          e ? (
            <KeyValueList
              items={[
                { key: t('preuves.reference'), value: e.id, mono: true },
                { key: t('preuves.etat'), value: t(`preuves.etat.${e.etat}`, { defaultValue: e.etat }) },
                { key: t('preuves.date'), value: e.dateExport },
                { key: t('preuves.empreinte'), value: <Hash value={e.empreinteManifeste} /> },
              ]}
            />
          ) : null
        }
      </Donnees>
      <h2 className="gm-card-title">{t('preuves.operations')}</h2>
      <OngletOperations liaison={liaison} avecPreuve />
    </div>
  )
}
