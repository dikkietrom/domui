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
package to.etc.domui.util.resources;

import org.eclipse.jdt.annotation.NonNull;

import java.util.List;

/**
 * The immutable dependencies of a resource.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jan 10, 2011
 */
final public class ResourceDependencies implements IIsModified {
	@NonNull
	final private IIsModified[] m_deplist;

	public ResourceDependencies(@NonNull IIsModified[] deplist) {
		m_deplist = deplist;
	}

	public ResourceDependencies(@NonNull List<IIsModified> deplist) {
		m_deplist = deplist.toArray(new IIsModified[deplist.size()]);
	}

	@Override
	public boolean isModified() {
		for(IIsModified m : m_deplist) {
			if(m.isModified())
				return true;
		}
		return false;
	}
}
