package oneny.jdbcspring.service;

import lombok.extern.slf4j.Slf4j;
import oneny.jdbcspring.domain.Member;
import oneny.jdbcspring.repository.MemberRepository;
import oneny.jdbcspring.repository.MemberRepositoryV3;
import oneny.jdbcspring.repository.MemberRepositoryV4_1;
import oneny.jdbcspring.repository.MemberRepositoryV4_2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * 트랜잭션 - DataSource, transactionManager
 */
@Slf4j
@SpringBootTest // 스프링 AOP를 적용하려면 스프링 컨테이너가 필요하다. 이 애노테이션이 있으면 테스트 시 스프링 부트를 통해 스프링 컨테이너를 생성한다.
  // 그리고 테스트에서 @Autowired 등을 통해 스프링 컨테이너가 관리하는 빈들을 사용할 수 있따.
class MemberServiceV4Test {

  public static final String MEMBER_A = "memberA";
  public static final String MEMBER_B = "memberB";
  public static final String MEMBER_EX = "ex";

  @Autowired
  private MemberRepository memberRepository;
  @Autowired
  private MemberServiceV4 memberService;

//  @BeforeEach
//  void setUp() {
//    DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
//    memberRepository = new MemberRepositoryV3(dataSource);
//
//    memberService = new MemberServiceV3_3(memberRepository);
//  }

  // 테스트 안에서 내부 설정 클래스를 만들어서 사용하면서 이 에노테이션을 붙이면, 스프링 부트가 자동으로 만들어주는 빈들에 추가로 필요한 스프링 빈들을 등록하고 테스트를 수행할 수 있다.
  @TestConfiguration
  static class TestConfig {

    private final DataSource dataSource;

    public TestConfig(DataSource dataSource) {
      this.dataSource = dataSource;
    }

    @Bean
    MemberRepository memberRepository() {
      return new MemberRepositoryV4_2(dataSource);
    }

    @Bean
    MemberServiceV4 memberServiceV4() {
      return new MemberServiceV4(memberRepository());
    }
  }

  @AfterEach
  void afterEach() {
    memberRepository.delete(MEMBER_A);
    memberRepository.delete(MEMBER_B);
    memberRepository.delete(MEMBER_EX);
  }

  @Test
  void AopCheck() {
    log.info("memberService class={}", memberService.getClass());
    log.info("memberRepository class={}", memberRepository.getClass());
    assertThat(AopUtils.isAopProxy(memberService)).isTrue();
  }

  @Test
  @DisplayName("정상 이체")
  void accountTransfer() {
    // given
    Member memberA = new Member(MEMBER_A, 10000);
    Member memberB = new Member(MEMBER_B, 10000);
    memberRepository.save(memberA);
    memberRepository.save(memberB);

    // when
    memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 2000);

    // then
    Member findMemberA = memberRepository.findById(memberA.getMemberId());
    Member findMemberB = memberRepository.findById(memberB.getMemberId());

    assertAll(
            () -> assertThat(findMemberA.getMoney()).isEqualTo(8000),
            () -> assertThat(findMemberB.getMoney()).isEqualTo(12000)
    );
  }

  @Test
  @DisplayName("이체 중 예외 발생")
  void accountTransferEx() {
    // given
    Member memberA = new Member(MEMBER_A, 10000);
    Member memberEx = new Member(MEMBER_EX, 10000);
    memberRepository.save(memberA);
    memberRepository.save(memberEx);

    // when
    assertThatThrownBy(() -> memberService.accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 2000))
            .isInstanceOf(IllegalStateException.class);

    // then
    Member findMemberA = memberRepository.findById(memberA.getMemberId());
    Member findMemberB = memberRepository.findById(memberEx.getMemberId());

    assertAll(
            () -> assertThat(findMemberA.getMoney()).isEqualTo(10000),
            () -> assertThat(findMemberB.getMoney()).isEqualTo(10000)
    );
  }
}
