import { SerieTemporelle } from '@gimac/design-system'

// Mesures d'exemple sur 6 h, une toutes les 15 min ; un trou (service redémarré) sur la série « document ».
const T0 = Date.UTC(2026, 9, 6, 6, 0)
const pas = 15 * 60_000
const courbe = (f: (i: number) => number | null) => Array.from({ length: 25 }, (_, i) => ({ t: T0 + i * pas, v: f(i) }))

export default function Apercu() {
  return (
    <div className="gm-col">
      <SerieTemporelle
        title="Charge processeur de la plateforme"
        subtitle="Somme des conteneurs, en % d'un cœur, 6 dernières heures"
        unit="%"
        seuil={80}
        series={[{ nom: 'Plateforme', points: courbe((i) => 32 + 14 * Math.sin(i / 3) + (i % 5)) }]}
      />
      <SerieTemporelle
        title="Latence p95 par service"
        subtitle="Millisecondes, 6 dernières heures"
        unit="ms"
        onExporter={() => {}}
        series={[
          { nom: 'gateway', points: courbe((i) => 120 + 30 * Math.sin(i / 4)) },
          { nom: 'iam', points: courbe((i) => 80 + 10 * Math.cos(i / 5)) },
          { nom: 'homologation', points: courbe((i) => 150 + (i > 18 ? 60 : 0)) },
          { nom: 'document', points: courbe((i) => (i >= 8 && i <= 10 ? null : 95 + (i % 7) * 4)) },
          { nom: 'participant', points: courbe((i) => 70 + (i % 4) * 3) },
        ]}
      />
    </div>
  )
}
