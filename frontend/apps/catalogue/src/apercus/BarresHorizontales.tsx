import { BarresHorizontales } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-row" style={{ alignItems: 'stretch' }}>
      <div style={{ flex: '1 1 420px' }}>
        <BarresHorizontales
          title="Comptes actifs par profil"
          unit="comptes"
          elements={[
            { libelle: 'Chargé DDRM', valeur: 9 },
            { libelle: 'Ingénieur d’homologation', valeur: 6 },
            { libelle: 'Correspondant de participant', valeur: 14 },
            { libelle: 'Chef du DES', valeur: 2 },
            { libelle: 'RSSI', valeur: 1 },
          ]}
        />
      </div>
      <div style={{ flex: '1 1 420px' }}>
        <BarresHorizontales
          title="Liaisons par étape du parcours"
          subtitle="Ordre du parcours"
          trier={false}
          elements={[
            { libelle: 'Cadrage', valeur: 4 },
            { libelle: 'Intégration', valeur: 7 },
            { libelle: 'Tests', valeur: 5 },
            { libelle: 'Certification', valeur: 4 },
            { libelle: 'Approbation sécurité', valeur: 2 },
            { libelle: 'Mise en production', valeur: 2 },
            { libelle: 'Suivi', valeur: 6 },
          ]}
        />
      </div>
    </div>
  )
}
