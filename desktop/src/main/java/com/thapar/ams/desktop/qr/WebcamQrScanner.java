package com.thapar.ams.desktop.qr;

import com.github.sarxos.webcam.Webcam;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.image.Image;

public final class WebcamQrScanner implements QrScanner {
  private final ExecutorService worker = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
  private volatile boolean active;
  private volatile Webcam webcam;

  @Override public void start(Consumer<Image> preview, Consumer<String> decoded, Consumer<Throwable> failure) {
    if (active) return;
    active = true;
    worker.submit(() -> {
      try {
        webcam = Webcam.getDefault();
        if (webcam == null) throw new IllegalStateException("No camera was found. Upload a QR image or enter the token.");
        webcam.open();
        while (active) {
          var frame = webcam.getImage();
          if (frame != null) {
            Image picture = QrCodec.toFxImage(frame);
            Platform.runLater(() -> { if (active) preview.accept(picture); });
            try {
              String token = QrCodec.decode(frame);
              active = false;
              Platform.runLater(() -> decoded.accept(token));
            } catch (IllegalArgumentException ignored) { }
          }
          Thread.sleep(120);
        }
      } catch (Exception e) {
        if (active) Platform.runLater(() -> failure.accept(e));
      } finally {
        if (webcam != null && webcam.isOpen()) webcam.close();
        webcam = null;
        active = false;
      }
    });
  }

  @Override public void close() {
    active = false;
    worker.shutdownNow();
  }
}
