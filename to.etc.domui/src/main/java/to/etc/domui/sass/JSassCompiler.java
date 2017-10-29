package to.etc.domui.sass;

import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import io.bit3.jsass.OutputStyle;
import io.bit3.jsass.context.StringContext;
import io.bit3.jsass.importer.Import;
import to.etc.domui.parts.ParameterInfoImpl;
import to.etc.domui.util.resources.IResourceDependencyList;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.Writer;
import java.util.Collections;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 29-10-17.
 */
public class JSassCompiler implements ISassCompiler {
	@Override public void compiler(String rurl, Writer output, @Nonnull ParameterInfoImpl params, @Nonnull IResourceDependencyList rdl) throws Exception {
		/*
		 * Define resolvers: these resolve "filenames" in the scss to resources in the webapp.
		 */
		String basePath;
		int pos = rurl.lastIndexOf('/');
		if(pos == -1) {
			basePath = "";
		} else {
			basePath = rurl.substring(pos + 1);
		}

		JSassResolver jsr = new JSassResolver(params, basePath, rdl);

		Import file = jsr.resolve(rurl, rurl);
		File out = File.createTempFile("sass-out-", ".css");
		System.out.println("out " + out);

		Options opt = new Options();
		opt.setImporters(Collections.singletonList(jsr));
		opt.setOutputStyle(OutputStyle.EXPANDED);
		opt.setIndent("\t");
		opt.setLinefeed("\n");
		opt.setSourceMapEmbed(true);

		StringContext fc = new StringContext(file.getContents(), file.getImportUri(), out.toURI(), opt);
		Compiler co = new Compiler();
		Output res = co.compile(fc);
		String css = res.getCss();
		output.write(css);
		jsr.close();
	}

	@Override public boolean available() {
		return true;
	}
}
