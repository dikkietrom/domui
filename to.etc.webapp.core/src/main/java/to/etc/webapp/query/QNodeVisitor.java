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
package to.etc.webapp.query;

import org.eclipse.jdt.annotation.NonNull;

import java.util.List;

public interface QNodeVisitor {
	void visitCriteria(@NonNull QCriteria<?> qc) throws Exception;

	void visitSelection(@NonNull QSelection<?> s) throws Exception;

	void visitUnaryNode(@NonNull QUnaryNode n) throws Exception;

	void visitLiteral(@NonNull QLiteral n) throws Exception;

	void visitMulti(@NonNull QMultiNode n) throws Exception;

	void visitOrder(@NonNull QOrder o) throws Exception;

	void visitBetween(@NonNull QBetweenNode n) throws Exception;

	void visitPropertyComparison(@NonNull QPropertyComparison qPropertyComparison) throws Exception;

	void visitUnaryProperty(@NonNull QUnaryProperty n) throws Exception;

	void visitRestrictionsBase(@NonNull QCriteriaQueryBase<?, ?> n) throws Exception;

	void visitOrderList(@NonNull List<QOrder> orderlist) throws Exception;

	void visitSelectionItem(@NonNull QSelectionItem n) throws Exception;

	void visitSelectionColumn(@NonNull QSelectionColumn qSelectionColumn) throws Exception;

	void visitPropertySelection(@NonNull QPropertySelection qPropertySelection) throws Exception;

	void visitMultiSelection(@NonNull QMultiSelection n) throws Exception;

	void visitExistsSubquery(@NonNull QExistsSubquery<?> q) throws Exception;

	void visitSelectionSubquery(@NonNull QSelectionSubquery n) throws Exception;

	void visitSubquery(@NonNull QSubQuery<?, ?> n) throws Exception;

	void visitPropertyJoinComparison(@NonNull QPropertyJoinComparison qPropertyJoinComparison) throws Exception;

	void visitSqlRestriction(@NonNull QSqlRestriction v) throws Exception;

	void visitPropertyIn(@NonNull QPropertyIn n) throws Exception;
}
