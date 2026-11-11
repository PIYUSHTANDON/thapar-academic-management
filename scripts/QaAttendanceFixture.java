import java.sql.*;
import java.time.*;

// Test-only database fixture. Never used by the application or a local user's database.
class QaAttendanceFixture {

  public static void main(String[] args) throws Exception {
    if (
      args.length != 1 || !args[0].contains("/.tools/qa/")
    ) throw new IllegalArgumentException(
      "Only isolated QA databases are permitted"
    );
    try (
      var connection = DriverManager.getConnection(args[0], "sa", "");
      var statement = connection.prepareStatement(
        "update ams_classsession set starts_at=?, ends_at=? where id=(select min(id) from ams_classsession)"
      )
    ) {
      var now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
      statement.setObject(1, now.minusMinutes(5));
      statement.setObject(2, now.plusMinutes(55));
      if (statement.executeUpdate() != 1) throw new IllegalStateException(
        "Expected one QA attendance fixture"
      );
    }
  }
}
