function getVisiblePages(page, totalPages) {
  return Array.from({ length: totalPages }, (_, index) => index + 1).filter(
    (pageNumber) =>
      pageNumber === 1 ||
      pageNumber === totalPages ||
      Math.abs(pageNumber - page) <= 1,
  )
}

export default function PaginationControls({
  endItem,
  page,
  setPage,
  startItem,
  totalItems,
  totalPages,
}) {
  if (!totalItems) {
    return null
  }

  const visiblePages = getVisiblePages(page, totalPages)

  return (
    <div className="pagination-bar">
      <span>
        Showing {startItem} to {endItem} of {totalItems}
      </span>
      <div className="pagination-actions" aria-label="Pagination">
        <button
          type="button"
          disabled={page === 1}
          onClick={() => setPage(page - 1)}
        >
          Previous
        </button>
        {visiblePages.map((pageNumber, index) => {
          const previousPage = visiblePages[index - 1]
          const hasGap = previousPage && pageNumber - previousPage > 1

          return (
            <span className="pagination-page-group" key={pageNumber}>
              {hasGap ? <span className="pagination-ellipsis">...</span> : null}
              <button
                type="button"
                className={pageNumber === page ? 'active' : ''}
                aria-current={pageNumber === page ? 'page' : undefined}
                onClick={() => setPage(pageNumber)}
              >
                {pageNumber}
              </button>
            </span>
          )
        })}
        <button
          type="button"
          disabled={page === totalPages}
          onClick={() => setPage(page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  )
}
