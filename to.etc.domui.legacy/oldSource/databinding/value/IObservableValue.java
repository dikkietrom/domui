/*
 * DomUI Java User Interface library
 * Copyright (c) 2010 by Frits Jalvingh, Itris B.V.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * See the "sponsors" file for a list of supporters.
 *
 * The latest version of DomUI and related code, support and documentation
 * can be found at http://www.domui.org/
 * The contact for the project is Frits Jalvingh <jal@etc.to>.
 */
package to.etc.domui.databinding.value;

import org.eclipse.jdt.annotation.*;

import to.etc.domui.databinding.*;
import to.etc.domui.util.*;

public interface IObservableValue<T> extends IObservable<T, ValueChangeEvent<T>, IValueChangeListener<T>>, IReadWriteModel<T> {
	@NonNull
	public Class<T> getValueType();

	/**
	 * Return the current value of the observable.
	 * @return
	 */
	@Nullable
	@Override
	public T getValue() throws Exception;

	@Override
	public void setValue(@Nullable T value) throws Exception;
}
