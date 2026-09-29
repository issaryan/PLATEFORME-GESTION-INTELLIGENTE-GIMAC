import { Link } from '../../actions/Link/Link'
import { CorrelationIdChip } from '../../affichage/CorrelationIdChip/CorrelationIdChip'
import { useLibelle } from '../../i18n/LangueProvider'

export interface FooterProps {
  /** Version de la plateforme, par exemple « 1.0.0 ». */
  version?: string
  /** Adresse de la documentation d'utilisation. */
  documentation?: string
  /** Identifiant de corrélation de la dernière requête, à communiquer au support. */
  correlationId?: string
  /** Année du droit d'auteur ; l'année courante par défaut. */
  annee?: number
}

// Pied de page GIMAC commun aux trois couches (§6.3) : groupement, référence et version de la
// plateforme, classification, documentation et identifiant de corrélation de la dernière requête.
export function Footer(p: FooterProps) {
  const t = useLibelle()
  const annee = p.annee ?? new Date().getFullYear()
  return (
    <footer className="gm-footer">
      <div className="gm-footer-identite">
        <span className="gm-footer-marque">GIMAC</span>
        <span>{t('pied.groupement')}</span>
      </div>
      <div className="gm-footer-meta">
        <span>{p.version ? t('pied.plateformeVersion', { version: p.version }) : t('pied.plateforme')}</span>
        <span>{t('pied.classification')}</span>
        {p.documentation ? <Link href={p.documentation} external>{t('pied.documentation')}</Link> : null}
        {p.correlationId ? (
          <span className="gm-row" style={{ gap: 'var(--gm-space-1)' }}>
            {t('pied.derniereRequete')} <CorrelationIdChip id={p.correlationId} />
          </span>
        ) : null}
        <span>{t('pied.droits', { annee: String(annee) })}</span>
      </div>
    </footer>
  )
}
