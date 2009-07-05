package to.etc.domui.server.reloader;

import java.net.*;
import java.util.*;
import java.util.logging.*;


/**
 * The classloader used by the reloader. Classes matching the include
 * pattern are loaded using this classloader, and all of the files
 * thus accessed are registered with the Reloader. When the Reloader
 * determines that registered sources have changed it will discard the
 * current instance of this classloader, thereby invalidating all classes,
 * and discard all sessions.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on May 22, 2008
 */
public class ReloadingClassLoader extends URLClassLoader {
	static private final Logger LOG = Reloader.LOG;

	static private int m_nextid = 1;

	private Reloader m_reloader;

	final private int m_id;

	private ClassLoader m_rootLoader;

	/**
	 * The list of files used in constructing these classes.
	 */
	private final List<ResourceTimestamp> m_dependList = new ArrayList<ResourceTimestamp>();

	static private final synchronized int nextID() {
		return m_nextid++;
	}

	public ReloadingClassLoader(ClassLoader parent, Reloader r) {
		super(r.getUrls(), parent);
		m_reloader = r;
		m_id = nextID();
		m_rootLoader = getClass().getClassLoader();
		//		System.out.println("ReloadingClassLoader: new instance "+this+" created");
	}

	@Override
	public String toString() {
		return "reloader[" + m_id + "]";
	}


	private void addWatchFor(Class< ? > clz) {
		ResourceTimestamp rt = m_reloader.findClassSource(clz); // Try to locate,
		if(rt == null) {
			LOG.info("Cannot find source file for class=" + clz + "; changes to this class are not tracked");
			return;
		}
		LOG.finer("Watching " + rt.getRef());
		synchronized(m_reloader) {
			m_dependList.add(rt);
		}
	}

	List<ResourceTimestamp> getDependencyList() {
		synchronized(m_reloader) {
			return new ArrayList<ResourceTimestamp>(m_dependList);
		}
	}

	/**
	 * Main workhorse for loading.
	 *
	 * @see java.lang.ClassLoader#loadClass(java.lang.String)
	 */
	@Override
	synchronized public Class< ? > loadClass(String name, boolean resolve) throws ClassNotFoundException {
		//		System.out.println("reloadingLoader: input="+name);
		if(name.startsWith("java.") || name.startsWith("javax.") || !m_reloader.watchClass(name)) {
			return m_rootLoader.loadClass(name); // Delegate to the rootLoader.
		}

		//-- We need to watch this class..
		Class< ? > clz = findLoadedClass(name);
		if(clz == null) {
			//-- Must we handle this class?
			LOG.finer("Need to load class=" + name);

			//-- Try to find the path for the class resource
			try {
				clz = findClass(name);
				addWatchFor(clz); // Only called if loading worked
			} catch(ClassNotFoundException x) {
				//-- *this* loader cannot find it. 
				if(getParent() == null)
					throw x;
				clz = getParent().loadClass(name); // Try to load by parent,
			}
			if(clz == null)
				throw new ClassNotFoundException(name);
		} // else
		//System.out.println("reloadingLoader: got existing class "+clz);

		if(resolve)
			resolveClass(clz);
		//		System.out.println("rcl: loaded "+clz+" using "+clz.getClassLoader());
		return clz;
	}
}
