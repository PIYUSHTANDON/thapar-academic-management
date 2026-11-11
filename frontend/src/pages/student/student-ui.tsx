import { useEffect, useRef } from "react";
import { X } from "lucide-react";
export function StatusBadge({ status }: { status: string }) {
  return (
    <span className={"badge " + status.toLowerCase().replaceAll(" ", "-")}>
      <i />
      {status}
    </span>
  );
}
export function AnnouncementDialog({
  announcement: a,
  onClose,
}: {
  announcement: any;
  onClose: () => void;
}) {
  const ref = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    const previous = document.activeElement as HTMLElement;
    ref.current?.focus();
    const key = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
      if (e.key === "Tab") {
        e.preventDefault();
        ref.current?.focus();
      }
    };
    document.addEventListener("keydown", key);
    return () => {
      document.removeEventListener("keydown", key);
      previous?.focus();
    };
  }, [onClose]);
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <section
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="announcement-title"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          ref={ref}
          className="dialog-close"
          aria-label="Close announcement"
          onClick={onClose}
        >
          <X size={20} />
        </button>
        <p className="eyebrow">COURSE ANNOUNCEMENT</p>
        <h2 id="announcement-title">{a.title}</h2>
        <p className="muted">
          {a.course} · {a.faculty}
        </p>
        <p className="announcement-description">
          {a.description || "No additional description was provided."}
        </p>
        <small>
          {a.publishedAt
            ? new Date(a.publishedAt).toLocaleString("en-IN")
            : "Publication date not available"}
        </small>
      </section>
    </div>
  );
}
