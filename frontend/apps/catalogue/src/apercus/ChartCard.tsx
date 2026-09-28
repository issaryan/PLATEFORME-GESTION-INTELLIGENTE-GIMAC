import { ChartCard } from '@gimac/design-system'

export default function Apercu() {
  return (
    <div className="gm-col">
      <div className="gm-row" style={{ alignItems: 'stretch', flexWrap: 'nowrap' }}>
        <ChartCard
          title="Disponibilité des liaisons"
          subtitle="Moyenne mensuelle, en %"
          type="line"
          unit="%"
          threshold={95}
          min={90}
          max={100}
          width={560}
          height={200}
          labels={['avr.', 'mai', 'juin', 'juil.', 'août', 'sept.']}
          series={[
            { name: 'Émetteur', data: [97.8, 98.2, 96.1, 97.4, 98.9, 99.1] },
            { name: 'Acquéreur', data: [95.2, 94.6, 97.3, 96.8, 97.2, 97.9] },
            { name: 'Compensation', data: [99.4, 99.1, 99.6, 99.2, 99.5, 99.7] },
          ]}
          status="ok"
          statusLabel="au-dessus du seuil"
        />
        <ChartCard title="Respect des engagements S1" type="gauge" unit="%" threshold={95} width={280} series={[{ name: 'Engagements', data: [97.4] }]} />
      </div>
      <ChartCard
        title="Tickets ouverts par sévérité"
        subtitle="Par semaine"
        type="bar"
        width={940}
        height={200}
        labels={['S36', 'S37', 'S38', 'S39', 'S40']}
        series={[
          { name: 'S1', data: [1, 0, 2, 1, 1] },
          { name: 'S2', data: [4, 3, 5, 6, 3] },
          { name: 'S3', data: [9, 12, 8, 11, 10] },
          { name: 'S4', data: [14, 11, 16, 13, 9] },
        ]}
      />
    </div>
  )
}
