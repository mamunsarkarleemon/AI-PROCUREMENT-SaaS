import { useEffect, useState } from 'react'
import { errorMessage } from '../api/client'

interface State<T> {
  key: string
  data: T | null
  loading: boolean
  error: string | null
}

/**
 * Load data when a page opens, and again whenever `deps` change:
 *   const { data, loading, error, reload } = useApi(() => productApi.mine(), [])
 */
export function useApi<T>(loader: () => Promise<T>, deps: unknown[]) {
  const [version, setVersion] = useState(0)
  const key = `${JSON.stringify(deps)}#${version}`
  const [state, setState] = useState<State<T>>({ key, data: null, loading: true, error: null })

  // New deps -> show the loader right away (React allows this "adjust state while rendering" pattern).
  if (state.key !== key) {
    setState((s) => ({ ...s, key, loading: true, error: null }))
  }

  useEffect(() => {
    let alive = true
    loader().then(
      (data) => alive && setState((s) => ({ ...s, data, loading: false })),
      (err) => alive && setState((s) => ({ ...s, error: errorMessage(err), loading: false })),
    )
    return () => {
      alive = false
    }
    // The loader is re-created on every render; `key` captures when it really needs to run again.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key])

  return {
    data: state.data,
    loading: state.loading,
    error: state.error,
    setData: (data: T) => setState((s) => ({ ...s, data })),
    reload: () => setVersion((v) => v + 1),
  }
}
