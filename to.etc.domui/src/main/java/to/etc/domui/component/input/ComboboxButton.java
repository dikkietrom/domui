package to.etc.domui.component.input;

import to.etc.domui.component.buttons.SmallImgButton;
import to.etc.domui.component.event.INotifyEvent;
import to.etc.domui.component.layout.Window;
import to.etc.domui.dom.css.DisplayType;
import to.etc.domui.dom.css.PositionType;
import to.etc.domui.dom.html.IClicked;
import to.etc.domui.dom.html.IControl;
import to.etc.domui.dom.html.IValueChanged;
import to.etc.domui.dom.html.NodeBase;
import to.etc.domui.dom.html.UrlPage;
import to.etc.domui.util.DomUtil;
import to.etc.domui.util.Msgs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Control that behaves as {@link SmallImgButton} that has built in click handler that popups select list with predefined data to choose from.
 *
 * @author <a href="mailto:vmijic@execom.eu">Vladimir Mijic</a>
 * Created on Nov 26, 2012
 */
public class ComboboxButton<T> extends SmallImgButton implements IControl<T> {
	public static final int DEFAULT_COMBO_SIZE = 8;

	public enum HAlign {LEFT, MIDDLE, RIGHT}

	@Nullable
	private IValueChanged<ComboboxButton<T>> m_onValueChanged;

	@Nullable
	private List<T> m_data;

	@Nonnull
	private final ComboLookup<T> m_picker;

	private int m_size = DEFAULT_COMBO_SIZE;

	private int m_offsetX = 0;

	private int m_offsetY = 0;

	@Nullable
	private T m_selected;

	private boolean m_mandatory = true;

	@Nullable
	private INotifyEvent<ComboboxButton<T>, ComboLookup<T>> m_onBeforeShow;

	@Nonnull
	private HAlign m_halign = HAlign.LEFT;

	/**
	 * Control that drop down picker would use as base for vertical and horizontal alignment. Picker is always shown below base control, while horizontal alignment can be defined (see {@link ComboboxButton#setHalign(HAlign)}).
	 * If not set different, picker {@link SmallImgButton} is used as alignment base.
	 */
	@Nullable
	private NodeBase m_alignmentBase;

	/**
	 * ComboboxButton constructor. By default size of drop down list is ComboboxButton.DEFAULT_COMBO_SIZE.
	 */
	public ComboboxButton() {
		m_picker = new ComboLookup<>();
	}

	/**
	 * ComboboxButton constructor. By default size of drop down list is ComboboxButton.DEFAULT_COMBO_SIZE.
	 * @param data data for picker popup
	 */
	public ComboboxButton(@Nonnull List<T> data) {
		this(data, DEFAULT_COMBO_SIZE);
	}

	/**
	 * ComboboxButton constructor.
	 * @param data data for picker popup
	 * @param size size of drop down list
	 */
	public ComboboxButton(@Nonnull List<T> data, int size) {
		m_data = data;
		m_size = size;
		m_picker = new ComboLookup<>(m_data);
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
		m_picker.setHeight("auto");
		m_picker.setClicked(node -> handlePickerValueChanged());
		m_picker.setReturnPressed(node -> handlePickerValueChanged());

		List<T> data = m_data;
		if(m_selected != null) {
			m_picker.setValue(m_selected);
		} else if(data != null && data.size() > 0 && isMandatory()) {
			m_picker.setValue(data.get(0));
		}

		m_picker.setMandatory(isMandatory());

		m_picker.setSpecialAttribute("onblur", "$(this).css('display','none');$(this).triggerHandler('click')");

		if(getClicked() == null) {
			setClicked(m_defaultClickHandler);
		}

		appendAfterMe(m_picker);
		positionPicker();
	}

	@Nullable @Override public NodeBase getForTarget() {
		return m_picker.getForTarget();
	}

	void handlePickerValueChanged() throws Exception {
		appendJavascript("$('#" + m_picker.getActualID() + "').css('display', 'none');");
		m_selected = m_picker.getValue();
		IValueChanged<ComboboxButton<T>> onValueChanged = getOnValueChanged();
		if(onValueChanged != null) {
			onValueChanged.onValueChanged(this);
		}
	}

	private final @Nonnull
	IClicked<SmallImgButton> m_defaultClickHandler = new IClicked<SmallImgButton>() {
		@SuppressWarnings("synthetic-access")
		@Override
		public void clicked(@Nonnull SmallImgButton clickednode) throws Exception {
			INotifyEvent<ComboboxButton<T>, ComboLookup<T>> onBeforeShow = getOnBeforeShow();
			if(onBeforeShow != null) {
				onBeforeShow.onNotify(ComboboxButton.this, m_picker);
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
			alignBase = ComboboxButton.this;
		}

		m_picker.alignTopToBottom(alignBase, m_offsetY, false);
		switch(m_halign){
			case LEFT:
				m_picker.alignToLeft(alignBase, m_offsetX, false);
				break;
			case RIGHT:
				m_picker.alignToRight(alignBase, m_offsetX, false);
				break;
			case MIDDLE:
				m_picker.alignToMiddle(alignBase, m_offsetX, false);
				break;
			default:
				throw new IllegalStateException("Unknown horizontal alignment? Found : " + m_halign);
		}
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
	 * Specify custom offset x relative to picker btn. By default, popup is rendered under {@link ComboboxButton#getAlignmentBase()} control.
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
	 * Returns custom offset y relative to {@link ComboboxButton#getAlignmentBase()} control. To set horizontal alignment rule see {@link ComboboxButton#setHalign(HAlign)}.
	 * @return
	 */
	public int getOffsetY() {
		return m_offsetY;
	}

	/**
	 * Specify custom offset y relative to picker btn. By default, popup is rendered under picker button.
	 */
	public void setOffsetY(int offsetY) {
		if(m_offsetY != offsetY) {
			m_offsetY = offsetY;
			if(isBuilt()) {
				forceRebuild();
			}
		}
	}

	@Override
	public void setValue(@Nullable T value) {
		m_selected = value;
		if(m_picker != null) {
			m_picker.setValue(value);
		}
	}

	@Override
	public @Nullable
	T getValue() {
		return m_selected;
	}

	public @Nullable
	INotifyEvent<ComboboxButton<T>, ComboLookup<T>> getOnBeforeShow() {
		return m_onBeforeShow;
	}

	public void setOnBeforeShow(@Nullable INotifyEvent<ComboboxButton<T>, ComboLookup<T>> onBeforeShow) {
		m_onBeforeShow = onBeforeShow;
	}

	@Override
	public boolean isMandatory() {
		return m_mandatory;
	}

	@Override
	public void setMandatory(boolean mandatory) {
		if(m_mandatory == mandatory)
			return;
		m_mandatory = mandatory;
		if(isBuilt()) {
			forceRebuild();
		}
	}

	/**
	 * Horizontal alignment of dropdown popup. By default set to {@link HAlign#Left}.
	 * @return
	 */
	@Nonnull
	public HAlign getHalign() {
		return m_halign;
	}

	/**
	 * Sets picker select list horizontal alignment.
	 * <UL>
	 * <LI> {@link HAlign#LEFT} position list to be left aligned with {@link ComboboxButton#getAlignmentBase()} component.</LI>
	 * <LI> {@link HAlign#MIDDLE} position list to be center aligned with {@link ComboboxButton#getAlignmentBase()} component.</LI>
	 * <LI> {@link HAlign#RIGHT} position list to be right aligned with {@link ComboboxButton#getAlignmentBase()} component.</LI>
	 * </UL>
	 *
	 * If no other component is set to be {@link ComboboxButton#getAlignmentBase()}, this defaults to drop down button.
	 * In order to affect offset to this aligment rules use {@link ComboboxButton#setOffsetY(int)} and {@link ComboboxButton#setOffsetX(int)}.
	 * @param halign
	 */
	public void setHalign(@Nonnull HAlign halign) {
		m_halign = halign;
		if(isBuilt()) {
			positionPicker();
		}
	}

	/**
	 * see {@link ComboboxButton#m_alignmentBase}
	 */
	public @Nullable
	NodeBase getAlignmentBase() {
		return m_alignmentBase;
	}

	/**
	 * see {@link ComboboxButton#m_alignmentBase}
	 * @param halignmentBase
	 */
	public void setAlignmentBase(@Nullable NodeBase halignmentBase) {
		m_alignmentBase = halignmentBase;
	}

	/**
	 * FIXME Breaks encapsulation.
	 */
	@Nonnull
	public ComboLookup<T> getSelectControl() {
		return m_picker;
	}

	@Nonnull
	private List<T> getData() {
		List<T> data = m_data;
		if(null == data)
			throw new IllegalStateException("Data is null");
		return data;
	}

	public boolean hasData() {
		return m_data != null;
	}

	public void setData(@Nullable List<T> data) {
		if(m_data != data) {
			m_data = data;
			m_picker.setData(data);
			if(m_selected != null && (data == null || !data.contains(m_selected))) {
				m_selected = null;
				m_picker.setValue(null);
			}
		}
	}

	@Override
	@Nullable
	public IValueChanged<ComboboxButton<T>> getOnValueChanged() {
		return m_onValueChanged;
	}

	@Override
	public void setOnValueChanged(@Nullable IValueChanged< ? > onValueChanged) {
		m_onValueChanged = (IValueChanged<ComboboxButton<T>>) onValueChanged;
	}

	@Override
	public T getValueSafe() {
		return DomUtil.getValueSafe(this);
	}

	@Override
	public boolean isReadOnly() {
		return getDisplay() == DisplayType.NONE;
	}

	@Override
	public void setReadOnly(boolean ro) {
		setDisplay(ro ? DisplayType.NONE : null);
	}
}
