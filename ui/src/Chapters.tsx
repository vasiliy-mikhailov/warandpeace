import { useState } from 'react'

import { DataTable, EmptyNote, Loaded, PageHeader, Pill, Section } from 'ratchet-ui/components'
import type { Column } from 'ratchet-ui/components'

import type { Chapter } from './api.js'
import { useRead } from './useRead.js'

/**
 * THE TEXT ITSELF: 365 chapters across four books and two epilogues, as the sweep will walk them.
 *
 * FILTERED IN THE BROWSER RATHER THAN BY THE SERVER, deliberately. The whole list is 365 rows and
 * arrives in one small response; a search endpoint would be a second contract to keep and a second
 * thing to get wrong, to save a reader nothing they can perceive.
 */
export function Chapters() {
  const chapters = useRead<Chapter[]>('/api/chapters')
  const [book, setBook] = useState<string>('')

  const columns: Column<Chapter>[] = [
    { head: 'Book', cell: (row) => row.book },
    { head: 'Chapter', cell: (row) => row.chapter },
    {
      head: 'Paragraphs',
      align: 'right',
      cell: (row) => row.paragraphs,
    },
  ]

  return (
    <Loaded
      what="chapter list"
      failed={chapters.failed}
      value={chapters.value}
      header={
        <PageHeader
          title="Chapters"
          subtitle="The Maude translation, Gutenberg #2600, split the way the sweep reads it"
        />
      }
    >
      {(rows) => {
        const books = [...new Set(rows.map((row) => row.book))]
        const shown = book === '' ? rows : rows.filter((row) => row.book === book)
        return (
          <>
            <Section title="Books" gutter="body">
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <button
                  type="button"
                  onClick={() => setBook('')}
                  style={plain}
                >
                  <Pill tone={book === '' ? 'running' : 'quiet'}>
                    All {rows.length}
                  </Pill>
                </button>
                {books.map((each) => (
                  <button key={each} type="button" onClick={() => setBook(each)} style={plain}>
                    <Pill tone={book === each ? 'running' : 'quiet'}>
                      {each} {rows.filter((row) => row.book === each).length}
                    </Pill>
                  </button>
                ))}
              </div>
            </Section>

            <Section title={book === '' ? 'Every chapter' : book} gutter="heading">
              {shown.length === 0 ? (
                <div style={{ padding: '0 24px' }}>
                  <EmptyNote>No chapter in this book, which should not be possible.</EmptyNote>
                </div>
              ) : (
                <DataTable
                  rows={shown}
                  columns={columns}
                  rowKey={(row) => row.slug}
                  rowClassName="wp-row"
                />
              )}
            </Section>
          </>
        )
      }}
    </Loaded>
  )
}

const plain = {
  background: 'none',
  border: 'none',
  padding: 0,
  cursor: 'pointer',
  font: 'inherit',
} as const
