import { useEffect, useRef, useState } from "react";
import { Camera, ImageUp, X } from "lucide-react";
export function parseAttendanceToken(value: string) {
  let text = value.trim();
  try {
    const parsed = JSON.parse(text);
    if (typeof parsed.token !== "string") throw Error();
    text = parsed.token;
  } catch {
    if (text.startsWith("{"))
      throw Error("This QR code does not contain an attendance token.");
  }
  if (!/^[A-Za-z0-9_-]{43}$/.test(text))
    throw Error(
      "Enter a valid attendance token or scan a Faculty attendance QR.",
    );
  return text;
}
export default function QrScanner({
  onToken,
}: {
  onToken: (token: string) => void;
}) {
  const [camera, setCamera] = useState(false),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const video = useRef<HTMLVideoElement>(null);
  useEffect(() => {
    if (!camera) return;
    let cancelled = false;
    let controls: { stop: () => void } | undefined;
    import("@zxing/browser")
      .then(async ({ BrowserQRCodeReader }) => {
        const reader = new BrowserQRCodeReader();
        controls = await reader.decodeFromVideoDevice(
          undefined,
          video.current!,
          (result) => {
            if (result && !cancelled) {
              try {
                onToken(parseAttendanceToken(result.getText()));
                setCamera(false);
              } catch (e) {
                setError((e as Error).message);
              }
            }
          },
        );
        if (cancelled) controls.stop();
      })
      .catch(() => {
        setError(
          "Camera access is unavailable or denied. Upload a QR image or enter the token instead.",
        );
        setCamera(false);
      });
    return () => {
      cancelled = true;
      controls?.stop();
    };
  }, [camera, onToken]);
  async function image(file: File) {
    setBusy(true);
    setError("");
    let url = "";
    try {
      if (
        file.size > 5 * 1024 * 1024 ||
        !["image/png", "image/jpeg", "image/webp"].includes(file.type)
      )
        throw Error("Choose a PNG, JPEG or WebP QR image up to 5 MB.");
      const { BrowserQRCodeReader } = await import("@zxing/browser");
      url = URL.createObjectURL(file);
      const result = await new BrowserQRCodeReader().decodeFromImageUrl(url);
      onToken(parseAttendanceToken(result.getText()));
    } catch (e) {
      setError(
        e instanceof Error && e.message.includes("Choose")
          ? e.message
          : "Could not read an attendance QR from this image. Try a clearer image or manual entry.",
      );
    } finally {
      if (url) URL.revokeObjectURL(url);
      setBusy(false);
    }
  }
  return (
    <div className="qr-scanner">
      <div className="scanner-actions">
        <button
          type="button"
          className="secondary"
          onClick={() => {
            setError("");
            setCamera(!camera);
          }}
        >
          {camera ? <X size={18} /> : <Camera size={18} />}{" "}
          {camera ? "Close camera" : "Scan QR with camera"}
        </button>
        <label className="secondary qr-upload">
          <ImageUp size={18} />
          {busy ? "Reading QR…" : "Upload QR image"}
          <input
            type="file"
            aria-label="Attendance QR image"
            accept="image/png,image/jpeg,image/webp"
            onChange={(e) => {
              if (e.target.files?.[0]) image(e.target.files[0]);
              e.target.value = "";
            }}
          />
        </label>
      </div>
      {camera && (
        <video
          className="qr-video"
          ref={video}
          autoPlay
          muted
          playsInline
          aria-label="Attendance QR camera preview"
        />
      )}
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
