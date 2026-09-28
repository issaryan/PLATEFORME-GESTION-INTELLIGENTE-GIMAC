import { WallBoard } from '@gimac/design-system'

// Le mur d'écrans emploie le mode sombre par défaut (DS-GIMAC-01 §12.5, DES-17) :
// l'aperçu publié force data-theme="dark" sur la racine ; le cadre ci-dessous en reproduit l'effet.
export default function Apercu() {
  return (
    <div data-theme="dark" className="gm-root gm-pad" style={{ margin: 'calc(-1 * var(--gm-space-4))' }}>
      <WallBoard
        tiles={[
          { id: 'LIA-2026-017', name: 'Établissement de paiement A · émetteur', state: 'crit', value: '0', unit: 'tx/min', metric: 'Transactions', alerts: 1 },
          { id: 'LIA-2025-102', name: 'Banque B · acquéreur', state: 'warn', value: '6,2', unit: '%', metric: 'Taux de rejet (seuil 5 %)', alerts: 1 },
          { id: 'LIA-2024-051', name: 'Banque D · émetteur et acquéreur', state: 'ok', value: '184', unit: 'tx/min', metric: 'Transactions', alerts: 0 },
          { id: 'LIA-2023-008', name: 'Banque E · émetteur', state: 'ok', value: '212', unit: 'tx/min', metric: 'Transactions', alerts: 0 },
          { id: 'LIA-2026-021', name: 'Établissement de microfinance C', state: 'maint', value: '…', unit: '', metric: "Maintenance planifiée jusqu'à 16:00", alerts: 0 },
          { id: 'LIA-2022-033', name: 'Banque F · acquéreur', state: 'ok', value: '97', unit: 'tx/min', metric: 'Transactions', alerts: 0 },
        ]}
      />
    </div>
  )
}
