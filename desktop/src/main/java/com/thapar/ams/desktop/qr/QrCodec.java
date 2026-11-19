package com.thapar.ams.desktop.qr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.BarcodeFormat;
import com.thapar.ams.contract.AttendanceContracts.AttendanceToken;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javafx.scene.image.Image;

public final class QrCodec {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");

  private QrCodec() { }

  public static Image encode(AttendanceToken attendance) {
    return new Image(new ByteArrayInputStream(encodePng(attendance)));
  }

  public static byte[] encodePng(AttendanceToken attendance) {
    try {
      String payload = JSON.writeValueAsString(
        new Payload("thapar-attendance", attendance.token(), attendance.sessionId()));
      var matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 240, 240);
      var bytes = new ByteArrayOutputStream();
      ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "png", bytes);
      return bytes.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("Could not generate attendance QR.", e);
    }
  }

  public static String decode(File file) throws IOException {
    BufferedImage image = ImageIO.read(file);
    if (image == null) throw new IllegalArgumentException("Choose a readable PNG or JPEG QR image.");
    return decode(image);
  }

  public static String decode(BufferedImage image) {
    try {
      var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
      return parseToken(new MultiFormatReader().decode(bitmap).getText());
    } catch (NotFoundException e) {
      throw new IllegalArgumentException("No attendance QR was found in the image.", e);
    } catch (Exception e) {
      if (e instanceof IllegalArgumentException invalid) throw invalid;
      throw new IllegalArgumentException("Could not read this QR image.", e);
    }
  }

  public static String parseToken(String input) {
    String text = input == null ? "" : input.trim();
    if (text.startsWith("{")) {
      try {
        var payload = JSON.readTree(text);
        if (!"thapar-attendance".equals(payload.path("type").asText()))
          throw new IllegalArgumentException("This QR is not for Thapar attendance.");
        text = payload.path("token").asText();
      } catch (IOException e) {
        throw new IllegalArgumentException("This QR does not contain a valid attendance token.", e);
      }
    }
    if (!TOKEN.matcher(text).matches())
      throw new IllegalArgumentException("Enter a valid token or scan a faculty attendance QR.");
    return text;
  }

  public static Image toFxImage(BufferedImage image) {
    try {
      var bytes = new ByteArrayOutputStream();
      ImageIO.write(image, "png", bytes);
      return new Image(new ByteArrayInputStream(bytes.toByteArray()));
    } catch (IOException e) {
      throw new IllegalStateException("Could not display QR image.", e);
    }
  }

  private record Payload(String type, String token, long sessionId) { }
}
