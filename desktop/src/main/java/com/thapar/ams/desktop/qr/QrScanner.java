package com.thapar.ams.desktop.qr;

import java.util.function.Consumer;
import javafx.scene.image.Image;

public interface QrScanner extends AutoCloseable {
  void start(Consumer<Image> preview, Consumer<String> decoded, Consumer<Throwable> failure);
  @Override void close();
}
