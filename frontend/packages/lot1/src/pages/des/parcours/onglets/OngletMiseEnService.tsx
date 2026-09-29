import { useTranslation } from 'react-i18next'
import { InlineAlert } from '@gimac/design-system'
import type { DossierLiaison } from '../../../../modele/parcours'
import { useFicheSuivi } from '../../../../hooks/parcours'
import { Donnees } from '../../liaisons/communs'
import { CarteServiceSupplementaire, CarteSuiviExploitation } from '../decision/Exploitation'
import { CarteCouvertureMiseEnService, CarteFicheSuivi } from '../decision/FicheSuivi'

// Onglet « Mise en service » (étape 10, rangs 32 à 35) : fiche de suivi des services et constat, couverture
// de mise en service, suivi de l'exploitation, ouverture d'un service supplémentaire.

export function OngletMiseEnService({ dossier }: { dossier: DossierLiaison }) {
  const { t } = useTranslation()
  const fiche = useFicheSuivi(dossier.id)
  const enProduction = dossier.statut === 'EN_PRODUCTION'
  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-5)' }}>
      {!enProduction ? (
        <InlineAlert level="info" title={t('miseEnService.avant.titre')}>{t('miseEnService.avant.texte', { statut: t(`parcours.statut.${dossier.statut}`) })}</InlineAlert>
      ) : null}
      <Donnees requete={fiche}>{(f) => <CarteFicheSuivi dossier={dossier} fiche={f} />}</Donnees>
      <CarteCouvertureMiseEnService dossier={dossier} />
      <CarteSuiviExploitation dossier={dossier} />
      <CarteServiceSupplementaire dossier={dossier} />
    </div>
  )
}
