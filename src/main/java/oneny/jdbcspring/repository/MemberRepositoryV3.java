package oneny.jdbcspring.repository;

import lombok.extern.slf4j.Slf4j;
import oneny.jdbcspring.domain.Member;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.NoSuchElementException;

/**
 * 트랜잭션 - 트랜잭션 매니저
 * DataSourceUtils.getConnection() <- 커넥션 획득
 * DataSourceUtils.releaseConnection() <- 커넥션 닫기
 */
@Slf4j
public class MemberRepositoryV3 {

  private final DataSource dataSource;

  public MemberRepositoryV3(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  private Connection getConnection() throws SQLException {
//    Connection con = dataSource.getConnection();
    // 주의! 트랜잭션 동기화를 사용하려면 DatqSourceUtils를 사용해야 한다.
    // 트랜잭션 매니저는 내부에서 트랜잭션 동기화 매니저를 사용한다.
    // 트랜잭션 동기화 매니저는 관리하는 커넥션이 있으면 해당 커넥션을 반환한다.
    // 트랜잭션 동기화 매니저가 관리하는 커넥션이 없는 경우 새로운 커넥션을 반환한다.
    // 따라서 이전처럼 파라미터로 커넥션을 전달하지 않아도 된다.(MemberService에서 JDBC 구현 기술 누수 문제 해결)
    Connection con = DataSourceUtils.getConnection(dataSource);
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
    // 주의! - 트랜잭션 동기화를 사용하려면 DataSourceUtils를 사용해야 한다.
    // con.close()을 사용해서 직접 닫아버리면 커넥션이 유지되지 않는 무넺가 발생한다. 이 커넥션은 이후 로직은 물론이고, 트랜잭션 종료(커밋, 롤백)할 때까지 살아있어야 한다.
    // DataSourceUtils.releaseConnection()을 사용하면 커넥션을 바로 닫는 것이 아니라 트랜잭션을 사용하기 위해 동기화된 커넥션은 커넥션을 닫지 않고 그대로 유지해준다.
    // 트랜잭션 동기화 매니저가 관리하는 커넥션이 없는 경우 해당 커넥션을 닫는다.
    DataSourceUtils.releaseConnection(con, dataSource);
//    JdbcUtils.closeConnection(con);

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
