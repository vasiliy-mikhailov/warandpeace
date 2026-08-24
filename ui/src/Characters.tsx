import { DataTable, EmptyNote, Loaded, PageHeader, Section, Tally } from 'ratchet-ui/components'
import type { Column } from 'ratchet-ui/components'

import type { Badges, Character } from './api.js'
import { useRead } from './useRead.js'

/**
 * THE PRODUCT. One row per character, and every number on it is a count of readings that exist.
 *
 * NOTHING HERE INVENTS A CHARACTER. The list is whatever the sweep has produced pages for, so an
 * empty table is the honest state of a wiki nobody has run the extraction for yet, and it says so
 * in those words rather than showing a table of headings a reader would take for a failed load.
 */
export function Characters() {
  const characters = useRead<Character[]>('/api/characters')
  const badges = useRead<Badges>('/api/badges')

  const columns: Column<Character>[] = [
    {
      head: 'Character',
      cell: (row) => <a href={`/character/${row.slug}`}>{row.name}</a>,
    },
    { head: 'Books', align: 'right', cell: (row) => row.books },
    { head: 'Chapters', align: 'right', cell: (row) => row.chapters },
    { head: 'Appearances', align: 'right', cell: (row) => row.appearances },
  ]

  return (
    <Loaded
      what="character list"
      failed={characters.failed}
      value={characters.value}
      header={
        <PageHeader
          title="Characters"
          subtitle="One page per character, built from the chapters they appear in"
        />
      }
    >
      {(rows) => (
        <>
          <Section title="The corpus" gutter="body">
            <div style={{ display: 'flex', gap: 28, flexWrap: 'wrap' }}>
              <Tally value={badges.value?.chapters ?? '—'} label="chapters in the book" />
              <Tally value={rows.length} label="characters with a page" />
              <Tally
                value={badges.value?.reading ?? '—'}
                label="readings recorded"
                tone={badges.value !== null && badges.value.reading === 0 ? 'alarm' : 'good'}
              />
            </div>
          </Section>

          <Section title="Characters" gutter="heading">
            {rows.length === 0 ? (
              <div style={{ padding: '0 24px' }}>
                <EmptyNote>
                  No character has a page yet. The book is loaded and every chapter is readable;
                  what is missing is a sweep — nothing has read them and written the eight sections
                  a character page is made of.
                </EmptyNote>
              </div>
            ) : (
              <DataTable
                rows={rows}
                columns={columns}
                rowKey={(row) => row.slug}
                rowClassName="wp-row"
              />
            )}
          </Section>
        </>
      )}
    </Loaded>
  )
}
