package to.etc.domui.component.lookup.filter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.w3c.dom.Node;
import to.etc.domui.component.meta.ClassMetaModel;
import to.etc.domui.component.meta.MetaManager;
import to.etc.webapp.query.IIdentifyable;
import to.etc.webapp.query.QDataContext;
import to.etc.xml.DomTools;
import to.etc.xml.XmlWriter;

/**
 * Translates an IIdentifyable value to XML and vice versa
 *
 * @author <a href="mailto:ben.schoen@itris.nl">Ben Schoen</a>
 * @since 2/8/16.
 */
@NonNullByDefault
final class IIdentifyableTranslator implements ITranslator<IIdentifyable<?>> {

	@Nullable
	@Override
	public IIdentifyable<?> deserialize(QDataContext dc, Node node) throws Exception {
		Node valueNode = DomTools.nodeFind(node, VALUE);
		String value = DomTools.textFrom(valueNode);
		String className = DomTools.strAttr(valueNode, CLASS);
		Class<? extends IIdentifyable<?>> clz = (Class<? extends IIdentifyable<?>>) getClass().getClassLoader().loadClass(className);
		ClassMetaModel classMeta = MetaManager.findClassMeta(clz);
		Class<? extends IIdentifyable<?>> actualClass = (Class<? extends IIdentifyable<?>>) classMeta.getActualClass();
		return dc.get(actualClass, Long.valueOf(value));
	}

	@Override
	public boolean serialize(XmlWriter writer, Object o) throws Exception {
		if(o instanceof IIdentifyable<?>) {
			String className = o.getClass().getCanonicalName();
			writer.tag(VALUE, CLASS, className);
			writer.write(String.valueOf(((IIdentifyable<?>) o).getId()));
			writer.tagendnl();
			return true;
		}
		return false;
	}
}
