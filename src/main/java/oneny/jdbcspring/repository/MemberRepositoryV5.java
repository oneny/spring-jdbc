package oneny.jdbcspring.repository;

import lombok.extern.slf4j.Slf4j;
import oneny.jdbcspring.domain.Member;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;

/**
 * JdbcTemplate 사
 */
@Slf4j
public class MemberRepositoryV5 implements MemberRepository {

  private final JdbcTemplate template;

  public MemberRepositoryV5(DataSource dataSource) {
    this.template = new JdbcTemplate(dataSource);
  }

  @Override
  public Member save(Member member) {
    String sql = "insert into member(member_id, money) values (?, ?)";
    template.update(sql, member.getMemberId(), member.getMoney());
    return member;
  }

  @Override
  public Member findById(String memberId) {
    String sql = "select * from member where member_id = ?";

    return template.queryForObject(sql, mememberRowMapper(), memberId);
  }

  private RowMapper<Member> mememberRowMapper() {
    return (rs, rowNum) -> {
      Member member = new Member();
      member.setMemberId(rs.getString("member_id"));
      member.setMoney(rs.getInt("money"));
      return member;
    };
  }

  @Override
  public void update(String memberId, int money) {
    String sql = "update member set money=? where member_id=?";
    template.update(sql, money, memberId); // 업데이트된 로우 수 반환
  }

  @Override
  public void delete(String memberId) {
    String sql = "delete from member where member_id=?";
    template.update(sql, memberId);
  }
}
