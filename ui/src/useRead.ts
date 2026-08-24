import { useEffect, useState } from 'react'

import { read } from './api.js'

/**
 * READ ONCE, AND KEEP BOTH OUTCOMES SEPARATE.
 *
 * `Loaded` from ratchet-ui takes a `value` and a `failed` and distinguishes three states from them —
 * waiting, failed, arrived — so this hook's whole job is to produce that pair honestly and never
 * both at once. Deliberately not `useAsk`: that models a request the READER triggers and reports
 * whether it landed, which is the shape of a button, and these are page loads.
 */
export function useRead<T>(path: string): { value: T | null; failed: string | null } {
  const [value, setValue] = useState<T | null>(null)
  const [failed, setFailed] = useState<string | null>(null)

  useEffect(() => {
    let current = true
    setValue(null)
    setFailed(null)
    read<T>(path)
      .then((answer) => {
        if (current) {
          setValue(answer)
        }
      })
      .catch((why: unknown) => {
        if (current) {
          setFailed(why instanceof Error ? why.message : String(why))
        }
      })
    // A page the reader has already navigated away from must not set state, and must not overwrite
    // the state of the page they are now on.
    return () => {
      current = false
    }
  }, [path])

  return { value, failed }
}
