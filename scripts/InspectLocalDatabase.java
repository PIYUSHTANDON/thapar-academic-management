import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class InspectLocalDatabase {

  public static void main(String[] args) throws Exception {
    try (
      var c = DriverManager.getConnection(
        args[0] + ";ACCESS_MODE_DATA=r",
        "sa",
        System.getenv().getOrDefault("H2_PASSWORD", "")
      )
    ) {
      for (String table : List.of(
        "ams_user",
        "ams_faculty",
        "ams_student",
        "ams_course",
        "ams_enrollment",
        "ams_classsession",
        "ams_attendancerecord",
        "ams_assessment",
        "ams_submission",
        "ams_grade",
        "ams_message",
        "ams_notification",
        "ams_calendarevent",
        "ams_learningresource"
      )) {
        var md = MessageDigest.getInstance("SHA-256");
        int count = 0;
        try (
          var st = c.createStatement();
          var r = st.executeQuery("select * from " + table + " order by id")
        ) {
          while (r.next()) {
            count++;
            for (
              int i = 1;
              i <= r.getMetaData().getColumnCount();
              i++
            ) md.update(
              Objects.toString(r.getObject(i), "null").getBytes(
                StandardCharsets.UTF_8
              )
            );
          }
        }
        System.out.println(
          table +
            " count=" +
            count +
            " fingerprint=" +
            HexFormat.of().formatHex(md.digest())
        );
      }
      try (
        var st = c.prepareStatement(
          "select identifier,name,role,password_hash,local_only from ams_user where identifier=?"
        )
      ) {
        st.setString(1, "FACULTY001");
        try (var r = st.executeQuery()) {
          if (!r.next()) {
            System.out.println("FACULTY001 exists=false");
          } else {
            String hash = r.getString(4);
            boolean matches = new BCryptPasswordEncoder().matches(
              System.getenv("LOCAL_DEMO_PASSWORD"),
              hash
            );
            System.out.println(
              "FACULTY001 exists=true name=" +
                r.getString(2) +
                " role=" +
                r.getString(3) +
                " bcrypt=" +
                hash.startsWith("$2") +
                " passwordMatches=" +
                matches +
                " localOnly=" +
                r.getBoolean(5)
            );
          }
        }
      }
    }
  }
}
