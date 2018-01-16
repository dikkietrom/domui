package to.etc.domui.component.lookupform2.lookupcontrols;

import to.etc.webapp.query.QCriteria;

import javax.annotation.Nullable;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 3-12-17.
 */
public interface ILookupQueryBuilder<D> {
	<T> LookupQueryBuilderResult appendCriteria(QCriteria<T> criteria, @Nullable D lookupValue);
}
