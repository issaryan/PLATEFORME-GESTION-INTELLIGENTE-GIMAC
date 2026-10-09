import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { StatusBadge } from '@gimac/design-system'
import { EtatsRequete } from '@gimac/socle'
import type { DossierLiaison, TacheActeur } from '../../../modele/parcours'
import { useDossierPortail, useMesTachesPortail } from '../../../hooks/parcours'
import { EnTete, tonStatut, useTypeProjet } from './commun'

// Cadre commun des écrans d'action du portail : la liaison est lue par la projection du portail (une
// liaison d'une autre institution y est introuvable), l'en-tête rappelle la liaison et ramène au suivi.

export function CadreAction({ id, titre, etat, children }: { id: string; titre: string; etat?: ReactNode; children: (d: DossierLiaison) => ReactNode }) {
  const { t } = useTranslation()
  const typeProjet = useTypeProjet()
  const q = useDossierPortail(id)
  return (
    <EtatsRequete etat={q} emptyTitle={t('pp.introuvable')} errorTitle={t('pp.introuvable')} retryLabel={t('commun.reessayer')}>
      {(d) => (
        <div className="gm-col" style={{ gap: 'var(--gm-space-5)', maxWidth: 880 }}>
          <EnTete
            retour={{ href: `/portail/liaisons/${d.id}`, libelle: t('pp.retourSuivi', { reference: d.reference }) }}
            titre={titre}
            sousTitre={
              <span className="gm-row">
                <span className="gm-mono">{d.reference}</span>
                <span aria-hidden="true">·</span>
                <span>{typeProjet(d)}</span>
                <StatusBadge status={tonStatut[d.statut]} label={t(`parcours.statut.${d.statut}`)} />
              </span>
            }
            etat={etat}
          />
          {children(d)}
        </div>
      )}
    </EtatsRequete>
  )
}

/** Tâche du participant sur cette liaison pour l'un des rangs donnés (lue dans son tableau de bord). */
export function useTacheLiaison(liaisonId: string, filtre: (t: TacheActeur) => boolean) {
  const q = useMesTachesPortail()
  return { requete: q, tache: (q.data?.taches ?? []).find((x) => x.liaisonId === liaisonId && filtre(x)) }
}
