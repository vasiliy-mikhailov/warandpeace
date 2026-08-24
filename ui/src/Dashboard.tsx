import {
  EmptyNote,
  Loaded,
  PageHeader,
  Pill,
  ProgressBar,
  Section,
  Tally,
} from 'ratchet-ui/components'
import { checkHealth } from 'ratchet-ui/check'

import type { Badges, Chapter, Health } from './api.js'
import { useRead } from './useRead.js'

/**
 * THE MACHINE THAT MAKES THE WIKI, as against the wiki itself.
 *
 * The two are served from one process on purpose, so a page and the coverage behind it cannot
 * drift apart. What this screen answers is the question the other two cannot: how much of the book
 * has actually been read, and is the reader that says so healthy.
 *
 * A ZERO HERE IS NOT A FAILURE AND IS NOT DRESSED AS ONE. The sweep is behind a compose profile
 * precisely so that restarting this reader cannot start spending a model budget, so "nothing read
 * yet" is the designed resting state of a fresh deployment and the page says which command changes
 * it rather than showing an alarm.
 */
export function Dashboard() {
  const badges = useRead<Badges>('/api/badges')
  const health = useRead<Health>('/api/health')
  const chapters = useRead<Chapter[]>('/api/chapters')

  const SECTIONS = 8

  return (
    <Loaded
      what="run"
      failed={badges.failed}
      value={badges.value}
      header={<PageHeader title="The run" subtitle="Lanes, coverage, and the record behind them" />}
    >
      {(counts) => {
        const total = counts.chapters * Math.max(counts.characters, 1) * SECTIONS
        const pct = total === 0 ? 0 : Math.min(100, (counts.reading / total) * 100)
        return (
          <>
            <Section title="Coverage" gutter="body">
              <div style={{ display: 'flex', gap: 28, flexWrap: 'wrap', marginBottom: 14 }}>
                <Tally value={counts.chapters} label="chapters" />
                <Tally value={counts.characters} label="characters" />
                <Tally
                  value={counts.reading}
                  label="readings"
                  tone={counts.reading === 0 ? 'alarm' : 'good'}
                />
                <Tally
                  value={total === 0 ? '—' : `${pct.toFixed(1)}%`}
                  label="of the readings a full sweep would produce"
                />
              </div>
              <ProgressBar pct={pct} />
              {counts.reading === 0 ? (
                <EmptyNote>
                  Nothing has been read yet. The sweep is deliberately not started by{' '}
                  <code>compose up</code> — it sits behind a profile, so restarting this reader
                  cannot begin spending a model budget by accident. Start one with{' '}
                  <code>docker compose run --rm wp-sweep &lt;character&gt; &lt;chapter&gt;
                  &lt;section&gt;</code>.
                </EmptyNote>
              ) : null}
            </Section>

            <Section title="The reader" gutter="body">
              <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
                {health.value === null ? (
                  <Pill tone="quiet">reading…</Pill>
                ) : health.value.ok ? (
                  <Pill tone="good" title="the process answering this page">
                    serving {health.value.version}
                  </Pill>
                ) : (
                  <Pill tone="alarm" title={health.value.why}>
                    unhealthy
                  </Pill>
                )}
                {/*
                  THE HEALTH DOCUMENT IS VALIDATED, NOT JUST READ. checkHealth is ratchet-ui's, and
                  a health endpoint whose own shape is wrong is the one endpoint you cannot find out
                  about by asking it how it is.
                */}
                {health.value !== null && checkHealth(health.value).length > 0 ? (
                  <Pill tone="alarm" title={checkHealth(health.value).join('; ')}>
                    health document off-contract
                  </Pill>
                ) : null}
              </div>
            </Section>

            <Section title="The corpus behind it" gutter="body">
              {chapters.value === null ? (
                <EmptyNote>Reading the chapter list…</EmptyNote>
              ) : (
                <div style={{ display: 'flex', gap: 28, flexWrap: 'wrap' }}>
                  <Tally
                    value={[...new Set(chapters.value.map((c) => c.book))].length}
                    label="books"
                  />
                  <Tally value={chapters.value.length} label="chapters loaded" />
                  <Tally
                    value={chapters.value
                      .reduce((sum, c) => sum + c.paragraphs, 0)
                      .toLocaleString('en')}
                    label="paragraphs"
                  />
                  <Tally value={SECTIONS} label="sections per chapter, per character" />
                </div>
              )}
            </Section>
          </>
        )
      }}
    </Loaded>
  )
}
