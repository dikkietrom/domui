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
package to.etc.domui.util.images.converters;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

import to.etc.domui.util.images.cache.*;
import to.etc.domui.util.images.machines.*;
import to.etc.sjit.*;

public class BitmapConverter implements IImageConverter, IImageIdentifier {
	static private final Set<String> MIMES = new HashSet<String>();

	static {
		MIMES.add("image/gif");
		MIMES.add("image/png");
		MIMES.add("image/tiff");
		MIMES.add("image/jpg");
		MIMES.add("image/jpeg");
		MIMES.add("image/bmp");
		//		MIMES.add("image/");
		//		MIMES.add("image/");

	}

	public int accepts(String inputmime, List<IImageConversionSpecifier> conversions) throws Exception {
		if(!MIMES.contains(inputmime))
			return -1;

		//-- Check for supported converters.
		IImageConversionSpecifier ics = conversions.get(0);
		if(ics instanceof ImagePageSelect) {
			if(conversions.size() == 1)
				return -1;
			ics = conversions.get(1);
		}

		//-- Supported crud?
		if(ics instanceof ImageResize)
			return 2;
		return -1;
	}

	public void convert(ImageConverterHelper helper, List<IImageConversionSpecifier> convs) throws Exception {
		String targetMime = null;
		int sourcePage = 0;
		ImageResize resize = null;

		//-- Extract all operations I can do something with.
		while(convs.size() > 0) {
			IImageConversionSpecifier ics = convs.get(0);
			if(ics instanceof ImagePageSelect) {
				convs.remove(0);
				ImagePageSelect ips = (ImagePageSelect) ics;
				sourcePage = ips.getPageNumber();
			} else if(ics instanceof ImageConvert) {
				convs.remove(0);
				ImageConvert c = (ImageConvert) ics;
				targetMime = c.getTargetMime();
			} else if(ics instanceof ImageResize) {
				convs.remove(0);
				resize = (ImageResize) ics;
				if(resize.getTargetMime() != null)
					targetMime = resize.getTargetMime();
			} else
				break;
		}

		//-- All that can be done is known now, and the known operations have been removed from the converter queue.
		//		if(sourcePage != 0)						// FIXME We may be able to do this for TIFF and alikes when using imagemagick
		//			throw new IllegalStateException("Mime type "+helper.getSource().getMime()+" is unpaged, only page 0 is available");
		if(resize == null)
			throw new IllegalStateException("Not acceptable (not a resize) after accept() accepted the work??");

		//-- Calculate the proper width and height, respecting the aspect ratio of the source
		OriginalImagePage ip = helper.getSource().getData().getPage(sourcePage);
		Dimension d = ImaTool.resizeWithAspect(resize.getWidth(), resize.getHeight(), ip.getWidth(), ip.getHeight());

		if(targetMime == null) {
			if(helper.getSource().getMime().equals("image/jpeg") || helper.getSource().getMime().equals("image/jpg"))
				targetMime = "image/jpeg";
			else
				targetMime = "image/png";
		}
		ImageHandler ih = ImageManipulator.getImageHandler();
		ImageSpec tis = resize instanceof ImageThumbnail ? ih.thumbnail(helper, helper.getSource(), 0, d.width, d.height, targetMime) : ih.scale(helper, helper.getSource(), 0, d.width, d.height,
			targetMime);
		helper.setTarget(tis);
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Identify an image's characteristics					*/
	/*--------------------------------------------------------------*/
	/**
	 *
	 * @see to.etc.domui.util.images.converters.IImageIdentifier#identifyImage(java.io.File, java.lang.String)
	 */
	public OriginalImageData identifyImage(File src, String mime) {
		//-- Ask ImageMagick...
		ImageHandler ih = ImageManipulator.getImageHandler();
		try {
			List<OriginalImagePage> l = ih.identify(src); // Try to identify
			return new OriginalImageData(mime, l);
		} catch(Exception x) {
			return null;
		}
	}
}
