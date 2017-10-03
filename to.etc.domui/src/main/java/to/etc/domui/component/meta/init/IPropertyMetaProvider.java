package to.etc.domui.component.meta.init;

import to.etc.domui.component.meta.ClassMetaModel;
import to.etc.domui.component.meta.PropertyMetaModel;

import javax.annotation.Nonnull;

/**
 * Add extra data to a PropertyMetaModel - provided you accept the model and the class
 * model it belongs to.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 2-10-17.
 */
public interface IPropertyMetaProvider {
	default void beforeProperties(@Nonnull MetaInitContext context, @Nonnull ClassMetaModel classModel) throws Exception {}

	<T> void provide(@Nonnull MetaInitContext context, @Nonnull ClassMetaModel classModel, @Nonnull PropertyMetaModel<T> propertyModel) throws Exception;

	default void afterPropertiesDone(@Nonnull MetaInitContext context, @Nonnull ClassMetaModel classModel) throws Exception {}
}
