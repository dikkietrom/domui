package to.etc.domui.component.input;

import java.util.*;

import javax.annotation.*;

import to.etc.domui.component.buttons.*;
import to.etc.domui.component.layout.*;
import to.etc.domui.dom.css.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.util.*;

public class DropDownPicker<T> extends SmallImgButton {
	public enum HAlign {LEFT, MIDDLE, RIGHT}; 

	public interface IDropDownPickerAdjuster<T> {
		void onBeforeShow(ComboLookup<T> m_picker) throws Exception;
	}

	@Nullable
	private List<T> m_data;

	@Nullable
	private IValueSelected<T> m_onValueSelected;

	@Nonnull
	private final ComboLookup<T> m_picker;

	private int m_size = 8;

	private int m_offsetX = 0;

	private int m_offsetY = 0;

	@Nullable
	private T m_selected;

	private boolean m_mandatory = true;
	
	@Nullable
	private IDropDownPickerAdjuster<T> m_adjuster;
	
	private HAlign m_halign = HAlign.LEFT;

	/**
	 * Control that drop down picker would use as base for vertical and horizontal alignment. Picker is always shown below base control, while horizontal alignment can be defined (see {@link DropDownPicker#setHalign(HAlign)}).
	 * If not set different, picker {@link SmallImgButton} is used as alignment base. 
	 */
	@Nullable
	private NodeBase m_alignmentBase;
	
	/**
	 * DropDownPicker constructor. By default size of drop down list is 8.
	 */
	public DropDownPicker() {
		m_picker = new ComboLookup<T>();
	}

	/**
	 * DropDownPicker constructor. By default size of drop down list is 8.
	 * @param data data for picker popup
	 */
	public DropDownPicker(@Nonnull List<T> data) {
		this(data, 8);
	}

	/**
	 * DropDownPicker constructor.
	 * @param data data for picker popup
	 * @param size size of drop down list
	 */
	public DropDownPicker(@Nonnull List<T> data, int size) {
		m_data = data;
		m_size = size;
		m_picker = new ComboLookup<T>(m_data);
	}

	@Override
	public void createContent() throws Exception {
		super.createContent();
		if(getSrc() == null) {
			setSrc(Msgs.BTN_FIND);
		}
		m_picker.setDisplay(DisplayType.NONE);
		m_picker.setPosition(PositionType.ABSOLUTE);

		//we use this to calculate correct zIndex in drop down picker later
		NodeBase zIndexNode = getParentOfTypes(Window.class, UrlPage.class);

		if(zIndexNode == null) {
			zIndexNode = getParent();
		}

		if(zIndexNode.getZIndex() > Integer.MIN_VALUE) {
			m_picker.setZIndex(zIndexNode.getZIndex() + 10);
		} else {
			m_picker.setZIndex(10);
		}
		m_picker.setSize(m_size);
		m_picker.setClicked(new IClicked<NodeBase>() {

			@Override
			public void clicked(NodeBase node) throws Exception {
				handlePickerValueChanged();
			}
		});
		m_picker.setReturnPressed(new IReturnPressed<Select>() {

			@Override
			public void returnPressed(@Nonnull Select node) throws Exception {
				handlePickerValueChanged();
			}
		});

		if(m_selected != null) {
			m_picker.setValue(m_selected);
		} else if(m_data.size() > 0 && isMandatory()) {
			m_picker.setValue(m_data.get(0));
		}
		m_picker.setMandatory(isMandatory());
		m_picker.setSpecialAttribute("onblur", "$(this).css('display','none');$(this).triggerHandler('click')");

		if(getClicked() == null) {
			setClicked(m_defaultClickHandler);
		}

		appendAfterMe(m_picker);
		positionPicker();
	}

	void handlePickerValueChanged() throws Exception {
		appendJavascript("$('#" + m_picker.getActualID() + "').css('display', 'none');");
		m_selected = m_picker.getValue();
		IValueSelected<T> onValueSelected = getOnValueSelected();
		if(onValueSelected != null) {
			onValueSelected.valueSelected(m_selected);
		}
	}

	private final @Nonnull
	IClicked<SmallImgButton> m_defaultClickHandler = new IClicked<SmallImgButton>() {
		@SuppressWarnings("synthetic-access")
		@Override
		public void clicked(SmallImgButton clickednode) throws Exception {
			IDropDownPickerAdjuster<T> adjuster = getAdjuster();
			if(adjuster != null) {
				adjuster.onBeforeShow(m_picker);
			}
			positionPicker();
			appendJavascript("$('#" + m_picker.getActualID() + "').css('display', 'inline');");
			appendJavascript("$('#" + m_picker.getActualID() + "').focus();");
			if(m_picker.getSelectedIndex() >= 0) {
				appendJavascript("WebUI.makeOptionVisible('" + m_picker.getOption(m_picker.getSelectedIndex()).getActualID() + "');");
			}
		}
	};

	private void positionPicker() {
		NodeBase alignBase = getAlignmentBase();
		if(alignBase == null) {
			alignBase = DropDownPicker.this;
		}

		appendJavascript("WebUI.alignToTop('" + m_picker.getActualID() + "', '" + alignBase.getActualID() + "', " + m_offsetY + ");");
		switch(m_halign){
			case LEFT:
				appendJavascript("WebUI.alignToLeft('" + m_picker.getActualID() + "', '" + alignBase.getActualID() + "', " + m_offsetX + ");");
				break;
			case RIGHT:
				appendJavascript("WebUI.alignToRight('" + m_picker.getActualID() + "', '" + alignBase.getActualID() + "', " + m_offsetX + ");");
				break;
			case MIDDLE:
				appendJavascript("WebUI.alignToMiddle('" + m_picker.getActualID() + "', '" + alignBase.getActualID() + "', " + m_offsetX + ");");
				break;
			default:
				throw new IllegalStateException("Unknown horizontal alignment? Found : " + m_halign);
		}
	}

	public @Nullable
	IValueSelected<T> getOnValueSelected() {
		return m_onValueSelected;
	}

	/**
	 * Register listener for on value selected event.
	 * @param onValueSelected
	 */
	public void setOnValueSelected(@Nullable IValueSelected<T> onValueSelected) {
		m_onValueSelected = onValueSelected;
	}

	/**
	 * Returns size of drop down list.
	 * @return
	 */
	public int getSize() {
		return m_size;
	}

	public void setSize(int size) {
		m_size = size;
		if(m_picker != null) {
			m_picker.setSize(m_size);
		}
	}

	/**
	 * Returns custom offset x relative to picker btn.
	 * @return
	 */
	public int getOffsetX() {
		return m_offsetX;
	}

	/**
	 * Specify custom offset x relative to picker btn. By default, popup is rendered under {@link DropDownPicker#getAlignmentBase()} control.
	 * @param offsetX
	 */
	public void setOffsetX(int offsetX) {
		if(m_offsetX != offsetX) {
			m_offsetX = offsetX;
			if(isBuilt()) {
				forceRebuild();
			}
		}
	}

	/**
	 * Returns custom offset y relative to {@link DropDownPicker#getAlignmentBase()} control. To set horizontal alignment rule see {@link DropDownPicker#setHalign(HAlign)}.
	 * @return
	 */
	public int getOffsetY() {
		return m_offsetY;
	}

	/**
	 * Specify custom offset y relative to picker btn. By default, popup is rendered under picker button.
	 * @param offsetX
	 */
	public void setOffsetY(int offsetY) {
		if(m_offsetY != offsetY) {
			m_offsetY = offsetY;
			if(isBuilt()) {
				forceRebuild();
			}
		}
	}

	public void setSelectedValue(@Nullable T value) {
		m_selected = value;
		if(m_picker != null) {
			m_picker.setValue(value);
		}
	}

	public @Nullable
	T getSelectedValue() {
		return m_selected;
	}

	public @Nullable
	IDropDownPickerAdjuster<T> getAdjuster() {
		return m_adjuster;
	}

	public void setAdjuster(@Nullable IDropDownPickerAdjuster<T> adjuster) {
		m_adjuster = adjuster;
	}

	public boolean isMandatory() {
		return m_mandatory;
	}

	public void setMandatory(boolean mandatory) {
		if(m_mandatory == mandatory)
			return;
		m_mandatory = mandatory;
		if (isBuilt()){
			forceRebuild();
		}
	}

	/**
	 * Horizontal alignment of dropdown popup. By default set to {@link HAlign#Left}.
	 * @return
	 */
	public HAlign getHalign() {
		return m_halign;
	}

	/**
	 * Sets picker select list horizontal alignment.
	 * <UL>
	 * <LI> {@link HAlign#LEFT} position list to be left aligned with {@link DropDownPicker#getAlignmentBase()} component.</LI>
	 * <LI> {@link HAlign#MIDDLE} position list to be center aligned with {@link DropDownPicker#getAlignmentBase()} component.</LI>
	 * <LI> {@link HAlign#RIGHT} position list to be right aligned with {@link DropDownPicker#getAlignmentBase()} component.</LI>
	 * </UL>
	 * 
	 * If no other component is set to be {@link DropDownPicker#getAlignmentBase()}, this defaults to drop down button.
	 * In order to affect offset to this aligment rules use {@link DropDownPicker#setOffsetY(int)} and {@link DropDownPicker#setOffsetX(int)}.  
	 * @param halign
	 */
	public void setHalign(@Nonnull HAlign halign) {
		m_halign = halign;
		if(isBuilt()) {
			positionPicker();
		}
	}

	/**
	 * see {@link DropDownPicker#m_alignmentBase}
	 * @param halignmentBase
	 */
	public @Nullable
	NodeBase getAlignmentBase() {
		return m_alignmentBase;
	}

	/**
	 * see {@link DropDownPicker#m_alignmentBase}
	 * @param halignmentBase
	 */
	public void setAlignmentBase(@Nullable NodeBase halignmentBase) {
		m_alignmentBase = halignmentBase;
	}

	public @Nonnull
	Select getSelectControl() throws Exception {
		return m_picker;
	}

	public @Nonnull
	List<T> getData() {
		return m_data;
	}

	public boolean hasData() {
		return m_data != null;
	}

	public void setData(@Nonnull List<T> data) {
		if(m_data != data) {
			m_data = data;
			m_picker.setData(data);
			if(m_selected != null && !data.contains(m_selected)) {
				m_selected = null;
				m_picker.setValue(null);
			}
		}
	}
}
