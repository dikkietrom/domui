package to.etc.webapp.query;

import org.eclipse.jdt.annotation.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * EXPERIMENTAL Default implementation of a before-image collecting cache, used for {@link QDataContext#original()}.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jan 13, 2014
 */
public class DefaultBeforeImageCache implements IBeforeImageCache {
	/** All copies that are not yet initialized. */
	@NonNull
	final private Set<Object> m_uninitializedSet = new HashSet<Object>();

	@NonNull
	final private Map<Object, Object> m_copyMap = new HashMap<Object, Object>();

	private boolean m_wasNew;

	@NonNull
	@Override
	public <T> T createImage(@NonNull Class<T> realclass, @NonNull T instance, boolean loaded) throws Exception {
//		System.out.println("   >> createImage " + realclass.getName() + "@" + System.identityHashCode(instance));
		T copy = (T) m_copyMap.get(instance);
		if(null != copy) {
			m_wasNew = false;
			if(loaded)
				m_uninitializedSet.remove(copy);			// No longer not initialized
			return copy;
		}

		//-- Make a new'un, then store it
		copy = realclass.newInstance();
		m_copyMap.put(instance, copy);
		if(!loaded)
			m_uninitializedSet.add(copy);
		m_wasNew = true;
		return copy;
	}

	@Override
	public boolean wasNew() {
		return m_wasNew;
	}

	@Override
	public <T> T findBeforeImage(@NonNull T source) {
		T res = (T) m_copyMap.get(source);
//		System.out.println("     >> " + source + " maps to " + res);
		return res;
	}

	@Override
	public <T> T getBeforeImage(@NonNull T instance) {
		T copy = (T) m_copyMap.get(instance);
		if(null == copy)
			return null;
		if(!isLoaded(copy))
			throw new IllegalStateException("Trying to get the before image of an unloaded instance");
		return null;
	}

	@Override
	public <T> boolean isLoaded(@NonNull T beforeImage) {
		return !m_uninitializedSet.contains(beforeImage);
	}
}
