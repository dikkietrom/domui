package to.etc.domui.util.exporters;

import javax.annotation.DefaultNonNull;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 26-10-17.
 */
@DefaultNonNull
public interface IExportFormat {
	/** The descriptive name for the format */
	String name();

	/** The file extension without "." */
	String extension();

	/** The actual exporter */
	IExportWriter<?> createWriter();
}
