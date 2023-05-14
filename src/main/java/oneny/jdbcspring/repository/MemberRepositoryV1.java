package oneny.jdbcspring.repository;

import lombok.extern.slf4j.Slf4j;
import oneny.jdbcspring.domain.Member;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.NoSuchElementException;

/**
 * JDBC - DataSource 사용, JdbcUtils 사용
 */
@Slf4j
public class MemberRepositoryV1 {

  private final DataSource dataSource;

  public MemberRepositoryV1(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  private Connection getConnection() throws SQLException {
    Connection con = dataSource.getConnection();
    log.info("get connection={}, class={}", con, con.getClass());
    return con;
  }

  public Member save(Member member) throws SQLException {
    String sql = "insert into member(member_id, money) values (?, ?)";

    Connection con = null;
    PreparedStatement pstmt = null;

    try {
      con = getConnection();
      pstmt = con.prepareStatement(sql); // 데이터베이스에 전달한 SQL과 파라미터로 전달한 데이터들을 준비
      pstmt.setString(1, member.getMemberId());
      pstmt.setInt(2, member.getMoney());
      // executeUpdate 는 int를 반환하는데 영향받은 DB row 수를 반환한다.
      pstmt.executeUpdate(); // State르 통해 준비된 SQL을 커넥션을 통해 실제 데이터베이스에 전달한다.
      return member;
    } catch (SQLException e) {
      log.error("error ", e);
      throw e;
    } finally {
      close(con, pstmt, null);
    }
  }

  public Member findById(String memberId) throws SQLException {
    String sql = "select * from member where member_id = ?";

    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
      con = getConnection();
      pstmt = con.prepareStatement(sql);

      pstmt.setString(1, memberId);
      rs = pstmt.executeQuery();
      if (rs.next()) {
        Member member = new Member();
        member.setMemberId(rs.getString("member_id"));
        member.setMoney(rs.getInt("money"));
        return member;
      } else {
        throw new NoSuchElementException("member not found memberId=" + memberId);
      }
    } catch (SQLException e) {
      log.error("db error ", e);
      throw e;
    } finally {
      close(con, pstmt, rs);
    }
  }

  public void update(String memberId, int money) throws SQLException {
    String sql = "update member set money=? where member_id=?";

    Connection con = null;
    PreparedStatement pstmt = null;

    try {
      con = getConnection();
      pstmt = con.prepareStatement(sql);
      pstmt.setInt(1, money);
      pstmt.setString(2, memberId);
      int resultSize = pstmt.executeUpdate();
      log.info("resultSize={}", resultSize);
    } catch (SQLException e) {
      log.error("db error", e);
      throw e;
    } finally {
      close(con, pstmt, null);
    }
  }

  public void delete(String memberId) throws SQLException {
    String sql = "delete from member where member_id=?";

    Connection con = null;
    PreparedStatement pstmt = null;

    try {
      con = getConnection();
      pstmt = con.prepareStatement(sql);
      pstmt.setString(1, memberId);
      int resultSize = pstmt.executeUpdate();
      log.info("resultSize={}", resultSize);
    } catch (SQLException e) {
      log.error("db error", e);
      throw e;
    } finally {
      close(con, pstmt, null);
    }
  }

  private void close(Connection con, Statement stmt, ResultSet rs) {
    JdbcUtils.closeResultSet(rs);
    JdbcUtils.closeStatement(stmt);
    JdbcUtils.closeConnection(con);

//    if (rs != null) {
//      try {
//        rs.close();
//      } catch (SQLException e) {
//        log.info("error", e);
//      }
//    }
//
//    if (stmt != null) {
//      try {
//        stmt.close();
//      } catch (SQLException e) {
//        log.info("error", e);
//      }
//    }
//
//    if (con != null) {
//      try {
//        con.close();
//      } catch (SQLException e) {
//        log.info("error", e);
//      }
//    }
  }
}
