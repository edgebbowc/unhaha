package commu.unhaha;

import com.querydsl.jpa.impl.JPAQueryFactory;
import commu.unhaha.config.QuerydslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@SpringBootTest
@Transactional
public class QuerydslTest {

    @Autowired
    private EntityManager em;

    @Test
    public void search() throws Exception{
        //given
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        //when
        
        //then
    }
}
