package to.etc.domui.dom.html;

import to.etc.domui.component.binding.BindReference;
import to.etc.domui.component.binding.BindingDefinitionException;
import to.etc.domui.component.binding.ComponentPropertyBinding;
import to.etc.domui.component.binding.IBidiBindingConverter;
import to.etc.domui.component.input.ITypedControl;
import to.etc.domui.component.meta.MetaManager;
import to.etc.domui.component.meta.PropertyMetaModel;
import to.etc.domui.util.Documentation;
import to.etc.domui.util.DomUtil;
import to.etc.domui.util.IValueAccessor;
import to.etc.webapp.ProgrammerErrorException;

import javax.annotation.Nonnull;

/**
 * This helps with creating control bindings to properties.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 24-3-18.
 */
final public class BindingBuilder<C> {
	@Nonnull
	final private NodeBase m_control;

	@Nonnull
	final private PropertyMetaModel<C> m_controlProperty;

	BindingBuilder(@Nonnull NodeBase control, @Nonnull String controlProperty) {
		if(control == null)
			throw new IllegalArgumentException("The control cannot be null.");
		if(controlProperty.contains("."))
			throw new ProgrammerErrorException("You cannot bind a Control property dotted path, see " + Documentation.BINDING_NO_DOTTED_PATH);
		m_control = control;
		m_controlProperty = (PropertyMetaModel<C>) MetaManager.getPropertyMeta(control.getClass(), controlProperty);
	}

	public <M> ComponentPropertyBinding to(@Nonnull BindReference<?, M> ref) throws Exception {
		return to(ref.getInstance(), ref.getProperty());
	}

	public <T, M> ComponentPropertyBinding to(@Nonnull T instance, @Nonnull String property) throws Exception {
		if(instance == null || property == null)
			throw new IllegalArgumentException("The instance in a component bind request CANNOT be null!");
		return to(instance, MetaManager.getPropertyMeta(instance.getClass(), property));
	}

	/**
	 * Bind to a IValueAccessor and the given instance.
	 */
	public <T, M> ComponentPropertyBinding to(@Nonnull T instance, @Nonnull IValueAccessor<M> pmm) throws Exception {
		if(instance == null || pmm == null)
			throw new IllegalArgumentException("Parameters in a bind request CANNOT be null!");

		//-- Check: are the types of the binding ok?
		if(pmm instanceof PropertyMetaModel<?>) {
			PropertyMetaModel<?> p = (PropertyMetaModel<?>) pmm;
			Class<?> actualType = DomUtil.getBoxedForPrimitive(p.getActualType());
			Class<?> controlType = DomUtil.getBoxedForPrimitive(m_controlProperty.getActualType());

			if(controlType == Object.class) {
				//-- Type erasure, deep, deep sigh. Can the control tell us the actual type contained?
				if(m_control instanceof ITypedControl) {
					ITypedControl<?> typedControl = (ITypedControl<?>) m_control;
					controlType = DomUtil.getBoxedForPrimitive(typedControl.getActualType());
				}
			}

			/*
			 * For properties that have a generic type the Java "architects" do type erasure, so we cannot check anything. Type safe my ...
			 */
			if(actualType != Object.class && controlType != Object.class) {
				if(!actualType.isAssignableFrom(controlType))
					throw new BindingDefinitionException(toString(), actualType.getName(), controlType.getName());

				if(!controlType.isAssignableFrom(actualType))
					throw new BindingDefinitionException(toString(), actualType.getName(), controlType.getName());
			}
		}

		//-- Move the data now!
		ComponentPropertyBinding binding = new ComponentPropertyBinding(m_control, m_controlProperty, instance, pmm);
		binding.moveModelToControl();
		m_control.finishBinding(binding);
		return binding;
	}

	public <M> ConvertingBindingBuilder<C, M> convert(IBidiBindingConverter<C, M> converter) {
		return new ConvertingBindingBuilder<>(m_control, m_controlProperty, converter);
	}
}