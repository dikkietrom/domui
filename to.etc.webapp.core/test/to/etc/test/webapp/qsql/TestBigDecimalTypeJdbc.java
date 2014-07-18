package to.etc.test.webapp.qsql;

import java.math.*;
import java.util.*;

import javax.sql.*;

import org.junit.*;

import to.etc.webapp.qsql.*;
import to.etc.webapp.query.*;
import to.etc.webapp.testsupport.*;

/**
 * Tests added support for {@link BigDecimalType}.
 * // FIXME when there is a standard way of adding records to the database for test purposes we are going to change this tests, now it depends a bit on data in database
 *
 *
 * @author <a href="mailto:vmijic@execom.eu">Vladimir Mijic</a>
 * Created on Jul 18, 2014
 */
public class TestBigDecimalTypeJdbc {

	private static DataSource m_ds = TUtilTestProperties.getRawDataSource();

	private QDataContext m_dc;

	@Before
	public void setUp() throws Exception {
		m_dc = new JdbcDataContext(null, m_ds.getConnection());
	}

	@After
	public void tearDown() throws Exception {
		m_dc.close();
	}

	@Test
	public void testBigDecimalTypeMappingNonnull() throws Exception {
		List<DecadePaymentOrder> nonNullAmountOrders = m_dc.query(QCriteria.create(DecadePaymentOrder.class).isnotnull(DecadePaymentOrder.AMOUNT2).limit(5));
		for(DecadePaymentOrder order : nonNullAmountOrders) {
			BigDecimal amount2 = JdbcUtil.selectOne(m_dc.getConnection(), BigDecimal.class, "SELECT bdrg FROM v_dec_betaalopdrachten WHERE admn_id = ? and docnr = ?", order.getId()
				.getAdministrationID(), order.getId().getDocnr());
			Assert.assertEquals("jdbc gets same value as hibernate for bigdecimal nonnull ", amount2, order.getAmount2());

			JdbcAnyRecord rec = JdbcUtil.queryAnyOne(m_dc.getConnection(), "SELECT bdrg FROM v_dec_betaalopdrachten WHERE admn_id = ? and docnr = ? and bdrg = ?", order.getId().getAdministrationID(),
				order.getId().getDocnr(), order.getAmount2());
			Assert.assertNotNull("jdbc finds same record using bigdecimal amount", rec);
		}
	}

	@Test
	public void testBigDecimalTypeMappingNullable() throws Exception {
		List<DecadePaymentOrder> nullAmountOrders = m_dc.query(QCriteria.create(DecadePaymentOrder.class).isnull(DecadePaymentOrder.AMOUNT2).limit(5));
		for(DecadePaymentOrder order : nullAmountOrders) {
			BigDecimal amount2 = JdbcUtil.selectOne(m_dc.getConnection(), BigDecimal.class, "SELECT bdrg FROM v_dec_betaalopdrachten WHERE admn_id = ? and docnr = ?", order.getId()
				.getAdministrationID(), order.getId().getDocnr());
			Assert.assertEquals("jdbc gets same value as hibernate for bigdecimal null ", amount2, order.getAmount2());

			JdbcAnyRecord rec = JdbcUtil.queryAnyOne(m_dc.getConnection(), "SELECT * FROM v_dec_betaalopdrachten WHERE admn_id = ? and docnr = ? and bdrg is null",
				order.getId().getAdministrationID(), order.getId().getDocnr());
			Assert.assertNotNull("jdbc finds same record using bigdecimal as null check", rec);
			Assert.assertNull("jdbc reads null for bigdecimal", rec.getValue(BigDecimal.class, "bdrg"));
		}
	}

}
