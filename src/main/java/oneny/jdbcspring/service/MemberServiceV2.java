package oneny.jdbcspring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oneny.jdbcspring.domain.Member;
import oneny.jdbcspring.repository.MemberRepositoryV1;
import oneny.jdbcspring.repository.MemberRepositoryV2;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 트랜잭션 - 파라미터 연동, 풀을 고려한 종료
 */
@RequiredArgsConstructor
@Slf4j
public class MemberServiceV2 {

  private final DataSource dataSource;
  private final MemberRepositoryV2 memberRepository;

  public void accountTransfer(String fromId, String toId, int money) throws SQLException {
    Connection con = dataSource.getConnection();

    try {
      con.setAutoCommit(false); // 트랜잭션 시작
      // 비즈니스 로
      bizLogic(con, fromId, toId, money);
      con.commit(); // 성공 시 커밋
    } catch (Exception e) {
      con.rollback(); // 실패 시 롤백
      throw new IllegalStateException(e);
    } finally {
      release(con);
    }
  }

  private void bizLogic(Connection con, String fromId, String toId, int money) throws SQLException {
    Member fromMember = memberRepository.findById(con, fromId);
    Member toMember = memberRepository.findById(con, toId);

    memberRepository.update(con, fromId, fromMember.getMoney() - money);
    validation(toMember);
    memberRepository.update(con, toId, toMember.getMoney() + money);
  }

  private void release(Connection con) {
    if (con != null) {
      try {
        // 커넥션 풀 고
        con.setAutoCommit(true); // 풀에 돌려줄 떄는 autocommit을 true 상태로 돌려줘야 한다.
        con.close();
      } catch (Exception e) {
        log.info("error", e);
      }
    }
  }

  private void validation(Member toMember) {
    if (toMember.getMemberId().equalsIgnoreCase("ex")) {
      throw new IllegalStateException("이체 중 예외 발생");
    }
  }
}
