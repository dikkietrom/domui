package to.etc.dbutil;

import java.sql.*;
import java.util.*;

import javax.annotation.concurrent.*;
import javax.sql.*;

@ThreadSafe
public final class DbLockKeeper {

	private DataSource m_dataSource;

	private final static DbLockKeeper M_INSTANCE = new DbLockKeeper();

	private static final String TABLENAME = "SYS_SERVER_LOCKS";

	@GuardedBy("this")
	private static final Map<LockThreadKey, Lock> M_MAINTAINED_LOCKS = new HashMap<LockThreadKey, Lock>();

	public synchronized static DbLockKeeper getInstance() {
		if(M_INSTANCE.m_dataSource == null) {
			throw new RuntimeException("DbLockKeeper not yet initialized");
		}
		return M_INSTANCE;
	}

	private DbLockKeeper() {}


	/**
	 * Initializes the DbLockKeeper. Creates the required tables and sets the datasource. Should be called before the first use of this class.
	 * @param ds the datasource used to create the connections.
	 */
	public synchronized static void init(DataSource ds) {
		if(M_INSTANCE.m_dataSource != null) {
			throw new RuntimeException("DbLockKeeper is already initialized.");
		}
		M_INSTANCE.m_dataSource = ds;
		PreparedStatement ps = null;
		Connection dbc = null;
		try {
			dbc = ds.getConnection();
			ps = dbc.prepareStatement("create table " + TABLENAME + " ( LOCK_NAME varchar(60) not null primary key)");
			ps.executeUpdate();
			dbc.commit();
		} catch(Exception x) {
			//Exception ignored, Table is always created, fails when already present.
		} finally {
			try {
				if(ps != null)
					ps.close();
				if(dbc != null)
					dbc.close();
			} catch(Exception x) {}
		}

	}

	/**
	 * Method should be used to create a lock. It can be used to make sure that certain processes won't run at the same time
	 * on multiple servers. The method won't finish untill lock is given. 
	 * 
	 * IMPORTANT
	 * The lock must be released after execution of the code.  
	 * 
	 * @param lockName the name of the lock
	 * @throws Exception 
	 */
	public LockHandle lock(final String lockName) throws Exception {
		LockThreadKey key = new LockThreadKey(lockName, Thread.currentThread());
		Lock lock;
		synchronized(this) {
			lock = M_MAINTAINED_LOCKS.get(key);
		}
		if(lock != null) {
			return new LockHandle(lock);
		}
		Connection dbc = m_dataSource.getConnection();
		insertLock(lockName, dbc);
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = dbc.prepareStatement("select lock_name from " + TABLENAME + " where lock_name = '" + lockName + "' for update of lock_name");
			rs = ps.executeQuery();
			if(!rs.next()) {
				throw new Exception("Lock with name: " + lockName + " not acquired");
			}
			lock = new Lock(this, lockName, dbc);
			synchronized(this) {
				M_MAINTAINED_LOCKS.put(key, lock);
			}
			return new LockHandle(lock);
		} finally {
			//connection is not closed here to keep the lock. Is done by the release call;
			if(ps != null)
				ps.close();
			if(rs != null)
				rs.close();
		}
	}

	private synchronized void releaseLock(String lockName) {
		LockThreadKey key = new LockThreadKey(lockName, Thread.currentThread());
		Lock lock = M_MAINTAINED_LOCKS.remove(key);
		if(lock == null || !lock.isClosed()) {
			throw new IllegalStateException("Lock with name:" + lockName + " has already been closed");
		}
	}

	/**
	 * Tries to insert the lock in the database. Ignores exceptions.
	 * @param lockName the name of the used lock
	 * @param dbc Connection to use
	 */
	private void insertLock(final String lockName, final Connection dbc) {
		PreparedStatement ps = null;
		try {
			ps = dbc.prepareStatement("insert into " + TABLENAME + " (lock_name) values('" + lockName + "')");
			ps.executeUpdate();
			dbc.commit();
		} catch(Exception e) {
			//Exception ignored, LockName is always inserted, fails when already present.
		} finally {
			if(ps != null)
				try {
					ps.close();
				} catch(SQLException e) {}
		}
	}

	/**
	 * Class to function as a key in the maintained locks map of the outer class. 
	 */
	private static final class LockThreadKey {
		private String m_lockName;

		private Thread m_thread;

		public LockThreadKey(String lockName, Thread thread) {
			m_lockName = lockName;
			m_thread = thread;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((m_lockName == null) ? 0 : m_lockName.hashCode());
			result = prime * result + ((m_thread == null) ? 0 : m_thread.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			LockThreadKey other = (LockThreadKey) obj;
			if(m_lockName == null) {
				if(other.m_lockName != null)
					return false;
			} else if(!m_lockName.equals(other.m_lockName))
				return false;
			if(m_thread == null) {
				if(other.m_thread != null)
					return false;
			} else if(!m_thread.equals(other.m_thread))
				return false;
			return true;
		}

	}

	/**
	 * Class keeps an lock on the database. Only handles to this lock will be
	 *  distibuted to classes that require a database lock. When all handle are 
	 *  released the lock is also released.
	 */
	private static final class Lock {
		private Connection m_lockedConnection;

		@GuardedBy("m_keeper")
		private int m_lockCounter;

		private String m_lockName;

		private DbLockKeeper m_keeper;

		public Lock(DbLockKeeper keeper, String lockName, Connection lockedConnection) {
			m_lockedConnection = lockedConnection;
			m_lockName = lockName;
			m_keeper = keeper;
		}

		public boolean isClosed() {
			return m_lockedConnection == null;
		}

		@SuppressWarnings("synthetic-access")
		public void release() throws SQLException {
			synchronized(m_keeper) {
				m_lockCounter--;
				if(m_lockCounter == 0) {
					try {
						m_lockedConnection.rollback();
					} finally {
						m_lockedConnection.close();
						m_lockedConnection = null;
					}
					m_keeper.releaseLock(m_lockName);
				}
			}
		}

		void increaseCounter() {
			synchronized(m_keeper) {
				m_lockCounter++;
			}
		}

	}

	/**
	 * Handle for a specific lock. Multiple handles can be distributed for a single lock.
	 * This will only be the case when a lock is asked for the same thread multiple times. 
	 */
	public static final class LockHandle {
		private Lock m_lock;

		private boolean m_released;

		public LockHandle(Lock lock) {
			m_lock = lock;
			lock.increaseCounter();
		}

		/**
		 * If this handle is the last/only handle for a lock the lock is released. 
		 * @throws Exception when exception with releasing the lock occurs.
		 */
		public void release() throws Exception {
			if(m_released)
				throw new IllegalStateException("Lock already released");
			m_released = true;
			m_lock.release();
		}

	}

}
