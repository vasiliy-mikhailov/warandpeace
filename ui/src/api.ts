import type { Health, Manifest } from 'ratchet-ui/wire'

/**
 * WHAT THE READER SERVES, AS TYPES, and read through ratchet-ui's validators where it has them.
 *
 * The manifest and the health endpoint are ratchet-ui's own shapes and are checked with its
 * `checkManifest` / `checkHealth` rather than trusted. The three below are this wiki's own: a
 * character, a chapter and a badge count are not concepts the shared contract has, and inventing a
 * `WorkItem` shape for them would be the wrong kind of reuse — the contract's types describe units
 * of agent WORK, and a chapter of a novel is not one.
 */
export type Character = {
  slug: string
  name: string
  appearances: number
  chapters: number
  books: number
}

export type Chapter = {
  slug: string
  book: string
  chapter: string
  paragraphs: number
}

export type Badges = {
  characters: number
  chapters: number
  reading: number
}

export type { Health, Manifest }

/**
 * ONE FETCH, AND A FAILURE THAT SAYS WHICH ENDPOINT.
 *
 * `Loaded` renders whatever string comes back here, so a bare "Failed to fetch" would put a
 * sentence on the page that names nothing a reader could act on. Naming the path costs one
 * template literal and is the difference between a page that reports a problem and a page that
 * reports that there was one.
 */
export async function read<T>(path: string): Promise<T> {
  let answered: Response
  try {
    answered = await fetch(path, { headers: { accept: 'application/json' } })
  } catch (unreachable) {
    throw new Error(`${path} could not be reached: ${String(unreachable)}`)
  }
  if (!answered.ok) {
    throw new Error(`${path} answered ${answered.status}`)
  }
  try {
    return (await answered.json()) as T
  } catch (unreadable) {
    throw new Error(`${path} did not answer with JSON: ${String(unreadable)}`)
  }
}
