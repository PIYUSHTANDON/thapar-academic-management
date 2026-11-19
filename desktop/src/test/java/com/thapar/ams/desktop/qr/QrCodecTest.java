package com.thapar.ams.desktop.qr;

import static org.junit.jupiter.api.Assertions.*;

import com.thapar.ams.contract.AttendanceContracts.AttendanceToken;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class QrCodecTest {
  private static final String TOKEN = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopq";

  @Test void generatedQrDecodesToTheServerToken() throws Exception {
    var image = ImageIO.read(new ByteArrayInputStream(
      QrCodec.encodePng(new AttendanceToken(TOKEN, LocalDateTime.of(2026, 11, 18, 17, 0), 7))));
    assertEquals(TOKEN, QrCodec.decode(image));
  }

  @Test void rejectsOtherQrPayloads() {
    assertThrows(IllegalArgumentException.class,
      () -> QrCodec.parseToken("{\"type\":\"other\",\"token\":\"" + TOKEN + "\"}"));
    assertThrows(IllegalArgumentException.class, () -> QrCodec.parseToken("not-a-token"));
  }
}
