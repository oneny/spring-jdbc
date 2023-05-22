package oneny.jdbcspring.repository;

import lombok.extern.slf4j.Slf4j;
import oneny.jdbcspring.domain.Member;
import oneny.jdbcspring.repository.ex.MyDbException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.*;
import java.util.NoSuchElementException;

/**
 * SQLExceptionTranslator 추가
 */
@Slf4j
public class MemberRepositoryV4_2 implements MemberRepository {

  private final DataSource dataSource;
  private final SQLExceptionTranslator exTranslator;

  public MemberRepositoryV4_2(DataSource dataSource) {
    this.dataSource = dataSource;
    this.exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
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

  @Override
  public Member save(Member member) {
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
      throw exTranslator.translate("save", sql, e);
//      throw new MyDbException(e);
    } finally {
      close(con, pstmt, null);
    }
  }

  @Override
  public Member findById(String memberId) {
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
      throw exTranslator.translate("findById", sql, e);
    } finally {
      close(con, pstmt, rs);
    }
  }

  @Override
  public void update(String memberId, int money){
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
      throw exTranslator.translate("update", sql, e);
    } finally {
      close(con, pstmt, null);
    }
  }

  @Override
  public void delete(String memberId) {
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
      throw exTranslator.translate("delete", sql, e);
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
