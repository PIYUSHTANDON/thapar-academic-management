import { useState, type ReactNode } from "react";
import { ChevronLeft, ChevronRight, ArrowRight } from "lucide-react";
export type Event = { id: string; title: string; date: string; kind?: string };
export function Panel({
  title,
  icon,
  action,
  children,
  className = "",
}: {
  title: string;
  icon?: ReactNode;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={"panel " + className}>
      <div className="panel-heading">
        <h2>
          {icon}
          {title}
        </h2>
        {action}
      </div>
      {children}
    </section>
  );
}
export function Empty({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>;
}
export function CalendarWidget({
  events,
  large = false,
}: {
  events: Event[];
  large?: boolean;
}) {
  const today = new Date();
  const [month, setMonth] = useState(
    new Date(today.getFullYear(), today.getMonth(), 1),
  );
  const [selected, setSelected] = useState(dateKey(today));
  const days = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate(),
    offset = month.getDay();
  function move(n: number) {
    setMonth(new Date(month.getFullYear(), month.getMonth() + n, 1));
  }
  return (
    <section className={"panel calendar " + (large ? "large-calendar" : "")}>
      <div className="month-heading">
        <button aria-label="Previous month" onClick={() => move(-1)}>
          <ChevronLeft size={19} />
        </button>
        <h2>
          {month.toLocaleDateString("en-IN", {
            month: "long",
            year: "numeric",
          })}
        </h2>
        <button aria-label="Next month" onClick={() => move(1)}>
          <ChevronRight size={19} />
        </button>
      </div>
      <div className="calendar-grid">
        {["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"].map((d) => (
          <span className="weekday" key={d}>
            {d}
          </span>
        ))}
        {Array.from({ length: offset }, (_, i) => (
          <span key={"blank" + i} />
        ))}
        {Array.from({ length: days }, (_, i) => {
          const date = dateKey(
            new Date(month.getFullYear(), month.getMonth(), i + 1),
          );
          return (
            <button
              aria-label={date}
              aria-pressed={date === selected}
              key={date}
              className={
                (selected === date ? "selected " : "") +
                (date === dateKey(today) ? "today" : "")
              }
              onClick={() => setSelected(date)}
            >
              {i + 1}
              {events.some((e) => e.date === date && e.kind) ? (
                <span className="event-markers">
                  {[
                    ...new Set(
                      events
                        .filter((e) => e.date === date)
                        .map((e) => e.kind || "Event"),
                    ),
                  ].map((kind) => (
                    <i
                      key={kind}
                      className={"event-dot " + kind.toLowerCase()}
                    />
                  ))}
                </span>
              ) : (
                events.some((e) => e.date === date) && <i />
              )}
            </button>
          );
        })}
      </div>
      {events.some((e) => e.kind) && (
        <div className="event-legend">
          <span>
            <i className="assignment" />
            Assignment
          </span>
          <span>
            <i className="quiz" />
            Quiz
          </span>
          <span>
            <i className="event" />
            Event
          </span>
          <span>
            <i className="class" />
            Class
          </span>
        </div>
      )}
      <div className="calendar-events">
        <small>
          {new Date(selected + "T12:00:00").toLocaleDateString("en-IN", {
            day: "numeric",
            month: "short",
          })}
        </small>
        {events
          .filter((e) => e.date === selected)
          .map((e) => (
            <p key={e.id}>{e.title}</p>
          ))}
        {!events.some((e) => e.date === selected) && <p>No academic events</p>}
      </div>
    </section>
  );
}
export function dateKey(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}
export function TextLink({
  children,
  onClick,
}: {
  children: ReactNode;
  onClick: () => void;
}) {
  return (
    <button className="text-link" onClick={onClick}>
      {children}
      <ArrowRight size={16} />
    </button>
  );
}
