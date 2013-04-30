package to.etc.domui.databinding;

import javax.annotation.*;

/**
 * Static helper to create bindings using the IObservable model.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Apr 24, 2013
 */
public class Bind {
	@Nonnull
	final protected IObservableValue< ? > m_from;

	protected IObservableValue< ? > m_to;

	private IValueChangeListener< ? > m_fromListener;

	private IValueChangeListener< ? > m_toListener;

	/** If not null, the IUniConverter OR IJoinConverter which converts values for this binding. */
	@Nullable
	protected IUniConverter< ? , ? > m_converter;

	/**
	 * Unidirectional bind from a -&gt; b.
	 *
	 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
	 * Created on Apr 30, 2013
	 */
	public static class UniBind extends Bind {
		private IValueChangeListener< ? > m_fromListener;

		private UniBind(@Nonnull IObservableValue< ? > sourceo) {
			super(sourceo);
		}

		/**
		 * Belongs to a {@link Bind#from(Object, String)} call and defines the target side of
		 * an unidirectional binding. Changes in "from" move to "to", but changes in "to" do
		 * not move back.
		 *
		 * @param target
		 * @param property
		 * @return
		 */
		@Nonnull
		public <T, V> UniBind to(@Nonnull T target, @Nonnull String property) throws Exception {
			if(null == target || null == property)
				throw new IllegalArgumentException("target/property cannot be null");
			IObservableValue<V> targeto = (IObservableValue<V>) createObservable(target, property);
			m_to = targeto;
			addSourceListener(targeto);

			//-- Immediately move the value of source to target too 2
			moveSourceToTarget();
			return this;
		}

		public <F, T> UniBind convert(@Nonnull IUniConverter<F, T> converter) {
			m_converter = converter;
			return this;
		}
	}

	/**
	 * Bidirectional binding a &lt;--&gt; b.
	 *
	 *
	 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
	 * Created on Apr 30, 2013
	 */
	public static class JoinBind extends Bind {
		private IValueChangeListener< ? > m_fromListener;

		private JoinBind(@Nonnull IObservableValue< ? > sourceo) {
			super(sourceo);
		}

		/**
		 * Belongs to a {@link Bind#from(Object, String)} call and defines the target side of
		 * an unidirectional binding. Changes in "from" move to "to", but changes in "to" do
		 * not move back.
		 *
		 * @param target
		 * @param property
		 * @return
		 */
		@Nonnull
		public <T, V> JoinBind to(@Nonnull T target, @Nonnull String property) throws Exception {
			if(null == target || null == property)
				throw new IllegalArgumentException("target/property cannot be null");
			IObservableValue<V> targeto = (IObservableValue<V>) createObservable(target, property);
			m_to = targeto;
			addSourceListener(targeto);
			addTargetListener(m_from);

			//-- Immediately move the value of source to target too 2
			moveSourceToTarget();
			return this;
		}

		public <F, T> JoinBind convert(@Nonnull IJoinConverter<F, T> converter) {
			m_converter = converter;
			return this;
		}

	}

	private Bind(@Nonnull IObservableValue< ? > sourceo) {
		m_from = sourceo;
	}

	/**
	 * Start for an "unidirectional" binding: Bind.from().to();
	 * @param source
	 * @param property
	 * @return
	 */
	static public <T> UniBind from(@Nonnull T source, @Nonnull String property) {
		if(null == source || null == property)
			throw new IllegalArgumentException("source/property cannot be null");
		IObservableValue< ? > sourceo = createObservable(source, property);
		return new UniBind(sourceo);
	}

	/**
	 * Start for a bidirectional binding: if either side changes it updates the other. The
	 * other side is defined with {@link #to(Object, String)}.
	 * @param source
	 * @param property
	 * @return
	 */
	@Nonnull
	static public <T> JoinBind join(@Nonnull T source, @Nonnull String property) {
		if(null == source || null == property)
			throw new IllegalArgumentException("source/property cannot be null");
		IObservableValue< ? > sourceo = createObservable(source, property);
		return new JoinBind(sourceo);
	}

	protected void moveSourceToTarget() throws Exception {
		Object val = ((IObservableValue<Object>) m_from).getValue();
		IUniConverter<Object, Object> uc = (IUniConverter<Object, Object>) m_converter;
		if(null != uc) {
			val = uc.convertSourceToTarget(val);
		}
		((IObservableValue<Object>) m_to).setValue(val);
	}

	protected void moveTargetToSource() throws Exception {
		Object val = ((IObservableValue<Object>) m_to).getValue();
		IJoinConverter<Object, Object> uc = (IJoinConverter<Object, Object>) m_converter;
		if(null != uc) {
			val = uc.convertTargetToSource(val);
		}
		((IObservableValue<Object>) m_from).setValue(val);

	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Event listener.										*/
	/*--------------------------------------------------------------*/

	static public final class Listener {
		@Nonnull
		final private IBindingListener m_listener;

		private int m_recurse;

		public Listener(IBindingListener listener) {
			m_listener = listener;
		}

		@Nonnull
		public <T, V> Listener on(@Nonnull T instance, @Nonnull String... properties) {
			for(String s : properties) {
				IObservableValue<V> o = (IObservableValue<V>) createObservable(instance, s);
				o.addChangeListener(new IValueChangeListener<V>() {
					@Override
					public void handleChange(ValueChangeEvent<V> event) throws Exception {
						if(m_recurse > 0)
							return;
						try {
							m_recurse++;
							m_listener.valuesChanged();
						} finally {
							m_recurse--;
						}
					}
				});
			}

			return this;
		}
	}

	@Nonnull
	static public Listener listener(@Nonnull IBindingListener lsnr) {
		return new Listener(lsnr);
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Helper code.										*/
	/*--------------------------------------------------------------*/
	/**
	 * Add a listener on FROM, so that changes there propagate to TO.
	 * @param value
	 */
	protected <V> void addSourceListener(@Nonnull IObservableValue<V> value) {
		IValueChangeListener<V> ml = new IValueChangeListener<V>() {
			@Override
			public void handleChange(@Nonnull ValueChangeEvent<V> event) throws Exception {
				moveSourceToTarget();
			}
		};
		m_fromListener = ml;
		((IObservableValue<V>) m_from).addChangeListener(ml);
	}

	/**
	 * Add a listener on FROM, so that changes there propagate to TO.
	 * @param value
	 */
	protected <V> void addTargetListener(@Nonnull IObservableValue<V> value) {
		IValueChangeListener<V> ml = new IValueChangeListener<V>() {
			@Override
			public void handleChange(@Nonnull ValueChangeEvent<V> event) throws Exception {
				moveTargetToSource();
			}
		};
		m_toListener = ml;
		((IObservableValue<V>) m_to).addChangeListener(ml);
	}

	@Nonnull
	static private <T> IObservableValue< ? > createObservable(@Nonnull T source, @Nonnull String property) {
		if(source instanceof IObservableEntity) {
			IObservableEntity oe = (IObservableEntity) source;
			IObservableValue< ? > op = oe.observableProperty(property);
			return op;
		}

		throw new IllegalArgumentException("The class " + source.getClass() + " is not Observable.");
	}

}
