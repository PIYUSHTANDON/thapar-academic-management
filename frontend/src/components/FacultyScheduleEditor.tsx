import { useEffect, useState, type FormEvent } from "react";
import { Panel } from "./ui";
import { api, errorText } from "../services/api";
export default function FacultyScheduleEditor() {
  const [sessions, setSessions] = useState<any[]>([]),
    [selected, setSelected] = useState(""),
    [error, setError] = useState(""),
    [success, setSuccess] = useState(""),
    [busy, setBusy] = useState(false);
  async function load() {
    try {
      const r = await api.get("/faculty/schedule");
      setSessions(r.data);
      if (!selected && r.data.length) setSelected(String(r.data[0].id));
    } catch (e) {
      setError(errorText(e));
    }
  }
  useEffect(() => {
    load();
  }, []);
  const session = sessions.find((s) => String(s.id) === selected);
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.currentTarget));
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      await api.put("/faculty/schedule/" + selected, {
        startsAt: f.startsAt,
        endsAt: f.endsAt,
        room: f.room,
        cancelled: f.cancelled === "on",
      });
      await load();
      setSuccess("Class information updated");
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <Panel title="Manage class information">
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      {success && (
        <p className="toast" role="status">
          {success}
        </p>
      )}
      <label>
        Teaching session
        <select value={selected} onChange={(e) => setSelected(e.target.value)}>
          {sessions.map((s) => (
            <option key={s.id} value={s.id}>
              {s.course} · {new Date(s.startsAt).toLocaleString("en-IN")}
            </option>
          ))}
        </select>
      </label>
      {session && (
        <form className="stack-form" key={selected} onSubmit={submit}>
          <label>
            Class start
            <input
              type="datetime-local"
              name="startsAt"
              required
              defaultValue={session.startsAt.slice(0, 16)}
            />
          </label>
          <label>
            Class end
            <input
              type="datetime-local"
              name="endsAt"
              required
              defaultValue={session.endsAt.slice(0, 16)}
            />
          </label>
          <label>
            Class room
            <input
              name="room"
              required
              maxLength={100}
              defaultValue={session.room}
            />
          </label>
          <label className="checkbox">
            <input
              name="cancelled"
              type="checkbox"
              defaultChecked={session.status === "Cancelled"}
            />
            Cancel this class
          </label>
          <button className="primary" disabled={busy}>
            Save class information
          </button>
        </form>
      )}
    </Panel>
  );
}
