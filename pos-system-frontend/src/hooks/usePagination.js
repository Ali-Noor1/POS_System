import { useMemo, useState } from 'react'

export const DEFAULT_PAGE_SIZE = 10

export function usePagination(items, pageSize = DEFAULT_PAGE_SIZE, resetKey = '') {
  const [pageState, setPageState] = useState({ page: 1, pageSize, resetKey })
  const totalItems = items.length
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize))
  const storedPage =
    pageState.pageSize === pageSize && pageState.resetKey === resetKey ? pageState.page : 1
  const page = Math.min(storedPage, totalPages)

  const pageItems = useMemo(() => {
    const startIndex = (page - 1) * pageSize
    return items.slice(startIndex, startIndex + pageSize)
  }, [items, page, pageSize])

  function setPage(nextPage) {
    const pageNumber = Math.min(Math.max(Number(nextPage) || 1, 1), totalPages)
    setPageState({ page: pageNumber, pageSize, resetKey })
  }

  return {
    endItem: Math.min(page * pageSize, totalItems),
    page,
    pageItems,
    pageSize,
    setPage,
    startItem: totalItems ? (page - 1) * pageSize + 1 : 0,
    totalItems,
    totalPages,
  }
}
