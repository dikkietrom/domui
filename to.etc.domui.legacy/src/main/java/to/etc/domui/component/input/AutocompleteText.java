package to.etc.domui.component.input;

import to.etc.domui.dom.html.Select;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * String input that is connected to select control. Connected select control provides source strings for autocomplete functionality.
 *
 *
 * @author <a href="mailto:vmijic@execom.eu">Vladimir Mijic</a>
 * Created on Nov 7, 2012
 */
class AutocompleteText extends TextStr {

	@Nullable
	private Select m_select;

	/**
	 * Initialize client side javascript plugin to support component on client side. Needs to be executed when both input and connected select have assigned actualIds.
	 * @throws Exception
	 */
	void initializeJS() throws Exception {
		appendCreateJS("WebUI.initAutocomplete('" + getActualID() + "','" + getSelect().getActualID() + "')");
	}

	@Nonnull
	protected Select getSelect() {
		Select select = m_select;
		if(select == null) {
			throw new IllegalStateException(Select.class.getName() + " not connected to " + AutocompleteText.class.getName());
		}
		return select;
	}

	protected void setSelect(@Nonnull Select select) {
		m_select = select;
	}

	protected boolean hasSelect() {
		return m_select != null;
	}
}
