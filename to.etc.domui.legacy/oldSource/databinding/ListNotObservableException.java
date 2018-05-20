package to.etc.domui.databinding;

import org.eclipse.jdt.annotation.*;

/**
 * Thrown when a list property is observed but it does not contain an observable list.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Nov 1, 2013
 */
public class ListNotObservableException extends RuntimeException {
	public ListNotObservableException(@NonNull Class< ? > clz, @NonNull String property) {
		super("The property '" + property + "' in class '" + clz + "' does not contain a List that implements IObservableList.");
	}
}
