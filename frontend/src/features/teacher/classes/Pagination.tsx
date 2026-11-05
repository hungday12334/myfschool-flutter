import { ChevronLeft, ChevronRight } from "lucide-react";

export function Pagination({
  page,
  pageSize,
  total,
  noun,
  loading = false,
  onChange,
}: {
  page: number;
  pageSize: number;
  total: number;
  noun: string;
  loading?: boolean;
  onChange: (page: number) => void;
}) {
  const pageCount = Math.max(1, Math.ceil(total / pageSize));
  const start = Math.max(0, Math.min(page - 1, pageCount - 3));
  const pages = [...new Set([0, start, start + 1, start + 2, pageCount - 1])]
    .filter((p) => p < pageCount)
    .sort((a, b) => a - b);

  return (
    <nav className="class-pagination" aria-label={`Phân trang ${noun}`}>
      <span>
        Hiển thị {total ? page * pageSize + 1 : 0}–
        {Math.min((page + 1) * pageSize, total)} trên {total} {noun}
      </span>
      <div>
        <button
          type="button"
          aria-label="Trang trước"
          disabled={loading || page === 0}
          onClick={() => onChange(page - 1)}
        >
          <ChevronLeft size={18} />
        </button>
        {pages.map((p, index) => (
          <span className="class-page-item" key={p}>
            {index > 0 && p - pages[index - 1] > 1 && (
              <span className="class-page-ellipsis" aria-hidden="true">
                …
              </span>
            )}
            <button
              type="button"
              aria-label={`Trang ${p + 1}`}
              aria-current={p === page ? "page" : undefined}
              disabled={loading}
              onClick={() => onChange(p)}
            >
              {p + 1}
            </button>
          </span>
        ))}
        <button
          type="button"
          aria-label="Trang sau"
          disabled={loading || page + 1 >= pageCount}
          onClick={() => onChange(page + 1)}
        >
          <ChevronRight size={18} />
        </button>
      </div>
    </nav>
  );
}
